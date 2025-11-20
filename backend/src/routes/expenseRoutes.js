import express from 'express';
import {
  createExpense,
  updateExpense,
  deleteExpense,
  getExpenses,
} from '../controllers/expenseController.js';
import { authenticate } from '../middleware/authMiddleware.js';

const router = express.Router();

router.use(authenticate);

router.post('/', createExpense);
router.put('/:id', updateExpense);
router.delete('/:id', deleteExpense);
router.get('/', getExpenses);

export default router;
