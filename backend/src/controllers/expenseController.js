import mongoose from 'mongoose';
import { Expense } from '../models/Expense.js';

const validateExpenseInput = (data, isUpdate = false) => {
  const errors = [];

  const requiredFields = ['amount', 'description', 'date', 'category'];
  if (!isUpdate) {
    requiredFields.forEach((field) => {
      if (data[field] === undefined || data[field] === null || data[field] === '') {
        errors.push(`${field} is required`);
      }
    });
  }

  if (data.amount !== undefined && (typeof data.amount !== 'number' || data.amount <= 0)) {
    errors.push('amount must be a positive number');
  }

  const allowedCategories = ['Food', 'Travel', 'Shopping', 'Bills', 'Other'];
  if (data.category !== undefined && !allowedCategories.includes(data.category)) {
    errors.push(`category must be one of: ${allowedCategories.join(', ')}`);
  }

  if (data.date !== undefined) {
    const d = new Date(data.date);
    if (isNaN(d.getTime())) {
      errors.push('date must be a valid date');
    }
  }

  return errors;
};

export const createExpense = async (req, res) => {
  try {
    const errors = validateExpenseInput(req.body, false);
    if (errors.length > 0) {
      return res.status(400).json({ errors });
    }

    const { amount, description, date, category } = req.body;
    const userId = req.user?.id;
    if (!userId) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    const expense = await Expense.create({
      amount,
      description,
      date: new Date(date),
      category,
      userId,
    });

    res.status(201).json(expense);
  } catch (error) {
    console.error('Error creating expense:', error);
    res.status(500).json({ message: 'Server error while creating expense' });
  }
};

export const updateExpense = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user?.id;
    if (!userId) {
      return res.status(401).json({ message: 'Unauthorized' });
    }
    const errors = validateExpenseInput(req.body, true);
    if (errors.length > 0) {
      return res.status(400).json({ errors });
    }

    const updateData = { ...req.body };
    if (updateData.date) {
      updateData.date = new Date(updateData.date);
    }

    const expense = await Expense.findOneAndUpdate({ _id: id, userId }, updateData, {
      new: true,
      runValidators: true,
    });

    if (!expense) {
      return res.status(404).json({ message: 'Expense not found' });
    }

    res.json(expense);
  } catch (error) {
    console.error('Error updating expense:', error);
    res.status(500).json({ message: 'Server error while updating expense' });
  }
};

export const deleteExpense = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user?.id;
    if (!userId) {
      return res.status(401).json({ message: 'Unauthorized' });
    }
    const expense = await Expense.findOneAndDelete({ _id: id, userId });

    if (!expense) {
      return res.status(404).json({ message: 'Expense not found' });
    }

    res.status(204).send();
  } catch (error) {
    console.error('Error deleting expense:', error);
    res.status(500).json({ message: 'Server error while deleting expense' });
  }
};

export const getExpenses = async (req, res) => {
  try {
    const { category, startDate, endDate, includeSummary } = req.query;
    const userId = req.user?.id;
    if (!userId) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    // Filters for the expense list (respect category/date + user)
    const expensesFilter = { userId };

    if (category) {
      expensesFilter.category = category;
    }

    if (startDate || endDate) {
      expensesFilter.date = {};
      if (startDate) expensesFilter.date.$gte = new Date(startDate);
      if (endDate) expensesFilter.date.$lte = new Date(endDate);
    }

    const expenses = await Expense.find(expensesFilter).sort({ date: -1 });

    // Summary should always represent totals for the full history of this user,
    // independent of any category/date filters applied to the list.
    if (includeSummary === 'true') {
      const summaryPipeline = [
        { $match: { userId: new mongoose.Types.ObjectId(userId) } },
        {
          $group: {
            _id: '$category',
            total: { $sum: '$amount' },
          },
        },
      ];

      const summaryResult = await Expense.aggregate(summaryPipeline);
      const perCategory = {};
      let total = 0;
      summaryResult.forEach((item) => {
        perCategory[item._id] = item.total;
        total += item.total;
      });

      return res.json({
        expenses,
        summary: {
          perCategory,
          total,
        },
      });
    }

    res.json({ expenses });
  } catch (error) {
    console.error('Error fetching expenses:', error);
    res.status(500).json({ message: 'Server error while fetching expenses' });
  }
};
