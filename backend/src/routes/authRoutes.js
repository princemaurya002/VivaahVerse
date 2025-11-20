import express from 'express';
import { body } from 'express-validator';
import { loginUser, registerUser } from '../controllers/authController.js';

const router = express.Router();

const emailValidator = body('email')
  .isEmail()
  .withMessage('Valid email is required')
  .normalizeEmail();

const passwordValidator = body('password')
  .isLength({ min: 6 })
  .withMessage('Password must be at least 6 characters long');

router.post(
  '/signup',
  [
    body('name').trim().notEmpty().withMessage('Name is required'),
    emailValidator,
    passwordValidator,
  ],
  registerUser
);

router.post('/login', [emailValidator, passwordValidator], loginUser);

export default router;

