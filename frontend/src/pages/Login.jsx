import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { GoogleLogin } from '@react-oauth/google';
import { Eye, EyeOff } from 'lucide-react';
import authAPI from '../api/authAPI';
import { useAuthStore } from '../store/authStore';

export default function Login() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuthStore();
  
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [loginError, setLoginError] = useState(''); // For wrong credentials error

  // Show session expired message if redirected from token refresh failure
  const sessionExpired = searchParams.get('session') === 'expired';

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    // Clear field error when user starts typing
    if (errors[name]) {
      setErrors({ ...errors, [name]: '' });
    }
    // Clear login error when user starts typing
    if (loginError) {
      setLoginError('');
    }
  };

  const handleSubmit = async (e) => {
    // CRITICAL: Prevent default form submission and page reload
    e.preventDefault();
    e.stopPropagation();
    
    console.log('🚀 Form submitted');
    console.log('📧 Email:', formData.email);
    console.log('🔑 Password:', formData.password);
    console.log('📏 Password length:', formData.password.length);
    
    setErrors({});
    setLoginError(''); // Clear previous login errors
    
    // Validate inputs
    const newErrors = {};
    if (!formData.email.trim()) newErrors.email = 'Email is required';
    if (!formData.password) newErrors.password = 'Password is required';
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    
    setLoading(true);

    try {
      console.log('🔐 Attempting login with:', formData.email);
      
      // Direct fetch to bypass any axios interceptor issues
      const fetchResponse = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: formData.email, password: formData.password })
      });
      
      console.log('📡 Fetch response status:', fetchResponse.status);
      const response = await fetchResponse.json();
      console.log('📦 Login response:', response);

      if (response?.token) {
        // Store user data and tokens from API response
        console.log('✅ Login successful, storing tokens and navigating...');
        login(
          {
            id: response.id,
            email: response.email,
            firstName: response.firstName,
            lastName: response.lastName,
            role: response.role
          },
          response.token,
          response.refreshToken,
          response.expiresIn || 3600
        );
        
        toast.success('Login successful! Welcome back.');
        
        // Use React Router navigate for smooth client-side navigation (NO page reload)
        // Small delay to ensure state is updated before navigation
        setTimeout(() => {
          console.log('🔄 Navigating to home page...');
          navigate('/', { replace: true });
        }, 100);
      } else {
        toast.error('Login failed. Please check your credentials.');
        setLoading(false);
      }
    } catch (error) {
      console.error('❌ Login error:', error);
      console.error('❌ Error message:', error.message);
      
      // Show the actual error
      setLoginError('Login error: ' + error.message);
      setLoading(false);
    }
  };

  // Handle Google login
  const handleGoogleSuccess = (credentialResponse) => {
    setLoading(true);
    try {
      const token = credentialResponse.credential;
      // Send token to backend for verification and user creation
      authAPI.loginWithGoogle(token)
        .then((response) => {
          if (response?.token) {
            login(
              {
                id: response.id,
                email: response.email,
                firstName: response.firstName,
                lastName: response.lastName,
                role: response.role
              },
              response.token,
              response.refreshToken,
              response.expiresIn || 3600
            );
            toast.success('Google login successful!');
            navigate('/');
          }
        })
        .catch((error) => {
          console.error('Google login error:', error);
          toast.error('Google login failed. Please try again.');
        })
        .finally(() => setLoading(false));
    } catch (error) {
      console.error('Google login error:', error);
      toast.error('Google login failed. Please try again.');
      setLoading(false);
    }
  };

  // Handle Facebook login (placeholder)
  const handleFacebookLogin = () => {
    toast.info('Facebook login is not yet configured. Please use email login or Google OAuth.');
  };

  // Handle Microsoft login (placeholder)
  const handleMicrosoftLogin = () => {
    toast.info('Microsoft login is not yet configured. Please use email login or Google OAuth.');
  };

  return (
    <div className="min-h-[calc(100vh-200px)] flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full bg-white rounded-xl shadow-lg p-8">
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold text-gray-900">Welcome Back</h2>
          <p className="mt-2 text-gray-600">Sign in to your account</p>
        </div>

        {sessionExpired && (
          <div className="mb-6 p-4 bg-amber-50 border border-amber-200 rounded-lg">
            <p className="text-sm text-amber-800">
              Your session has expired. Please log in again.
            </p>

          {/* Forgot password link */}
          <div className="flex justify-end mt-1 mb-4">
            <a href="/forgot-password" className="text-sm text-primary-600 hover:text-primary-700 font-medium">Forgot password?</a>
          </div>
        </div>
        )}

        {/* Wrong credentials error message */}
        {loginError && (
          <div className="mb-6 p-4 bg-red-50 border border-red-300 rounded-lg">
            <div className="flex items-center">
              <svg className="w-5 h-5 text-red-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
              <p className="text-sm font-medium text-red-700">{loginError}</p>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              Email Address
            </label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              value={formData.email}
              onChange={handleChange}
              className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.email ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="you@example.com"
              disabled={loading}
            />
            {errors.email && (
              <p className="mt-1 text-sm text-red-600">{errors.email}</p>
            )}
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            <div className="relative">
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                value={formData.password}
                onChange={handleChange}
                className={`w-full px-4 py-2 pr-10 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                  errors.password ? 'border-red-500' : 'border-gray-300'
                }`}
                placeholder="••••••••"
                disabled={loading}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700 focus:outline-none"
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
            {errors.password && (
              <p className="mt-1 text-sm text-red-600">{errors.password}</p>
            )}
          </div>


          {/* Forgot password link - always visible */}
          <div className="flex justify-end mt-1 mb-4">
            <Link to="/forgot-password" className="text-sm text-primary-600 hover:text-primary-700 font-medium">Forgot password?</Link>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-primary-600 hover:bg-primary-700 disabled:bg-gray-400 text-white font-medium py-2 rounded-lg transition-colors"
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        {/* OAuth section disabled - Google OAuth not configured 
        <div className="mt-6 flex items-center">
          <div className="flex-1 border-t border-gray-300"></div>
          <span className="px-3 text-sm text-gray-500">Or continue with</span>
          <div className="flex-1 border-t border-gray-300"></div>
        </div>
        <div className="mt-6 space-y-3">
          <div className="flex justify-center">
            <GoogleLogin
              onSuccess={handleGoogleSuccess}
              onError={() => toast.error('Google login failed')}
              text="signin"
              width="100"
            />
          </div>
        </div>
        */}

        <div className="mt-6 text-center">
          <p className="text-sm text-gray-600">
            Forgot your password?{' '}
            <Link to="/forgot-password" className="text-blue-600 hover:text-blue-700 font-medium">
              Reset it here
            </Link>
          </p>
        </div>

        <div className="mt-4 text-center">
          <p className="text-sm text-gray-600">
            Don't have an account?{' '}
            <Link to="/register" className="text-primary-600 hover:text-primary-700 font-medium">
              Sign up
            </Link>
          </p>
        </div>

        <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <p className="text-xs font-semibold text-blue-900 mb-2">Demo Credentials:</p>
          <p className="text-xs text-blue-800"><strong>Email:</strong> test@example.com</p>
          <p className="text-xs text-blue-800"><strong>Password:</strong> password1</p>
          <p className="text-xs text-blue-700 mt-2">Or create a new account via Register</p>
        </div>
      </div>
    </div>
  );
}
