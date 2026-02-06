import { useState, useEffect } from 'react';
import api from '../../api/axios';

export default function Overview() {
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchOverview = async () => {
      try {
        const response = await api.get('/admin/stats');
        setOverview(response.data);
        setLoading(false);
      } catch (err) {
        console.error('Error fetching overview:', err);
        setError('Failed to load dashboard overview');
        setLoading(false);
      }
    };

    fetchOverview();
    // Refresh every 30 seconds
    const interval = setInterval(fetchOverview, 30000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return <div className="text-center py-8">Loading overview...</div>;
  }

  if (error) {
    return <div className="text-center py-8 text-red-600">{error}</div>;
  }

  if (!overview) {
    return <div className="text-center py-8">No data available</div>;
  }

  const StatCard = ({ label, value, icon, color }) => (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-gray-500 text-sm">{label}</p>
          <p className={`text-3xl font-bold ${color}`}>{value}</p>
        </div>
        <div className="text-4xl">{icon}</div>
      </div>
    </div>
  );

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900">Dashboard Overview</h2>
        <p className="text-gray-500 mt-1">Key metrics and statistics</p>
      </div>

      {/* User Stats */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">👥 User Statistics</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <StatCard
            label="Total Users"
            value={overview.totalUsers || 0}
            icon="👥"
            color="text-blue-600"
          />
          <StatCard
            label="Active Users"
            value={overview.activeUsers || 0}
            icon="✅"
            color="text-green-600"
          />
          <StatCard
            label="New This Month"
            value={overview.newUsersThisMonth || 0}
            icon="🆕"
            color="text-purple-600"
          />
        </div>
      </div>

      {/* Order Stats */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">🛒 Order Statistics</h3>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <StatCard
            label="Total Orders"
            value={overview.totalOrders || 0}
            icon="🛒"
            color="text-indigo-600"
          />
          <StatCard
            label="Pending Orders"
            value={overview.pendingOrders || 0}
            icon="⏳"
            color="text-yellow-600"
          />
          <StatCard
            label="Completed Orders"
            value={overview.completedOrders || 0}
            icon="✔️"
            color="text-green-600"
          />
          <StatCard
            label="Orders (Last 7 Days)"
            value={overview.recentOrdersCount || 0}
            icon="📈"
            color="text-blue-600"
          />
        </div>
      </div>

      {/* Revenue Stats */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">💰 Revenue Statistics</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <StatCard
            label="Total Revenue"
            value={`₹${(overview.totalRevenue || 0).toFixed(2)}`}
            icon="💵"
            color="text-green-600"
          />
          <StatCard
            label="This Month"
            value={`₹${(overview.revenueThisMonth || 0).toFixed(2)}`}
            icon="📊"
            color="text-blue-600"
          />
          <StatCard
            label="Average Order Value"
            value={`$${(overview.averageOrderValue || 0).toFixed(2)}`}
            icon="📈"
            color="text-purple-600"
          />
        </div>
      </div>

      {/* Product Stats */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">📦 Product Statistics</h3>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <StatCard
            label="Total Products"
            value={overview.totalProducts || 0}
            icon="📦"
            color="text-gray-600"
          />
          <StatCard
            label="Low Stock Items"
            value={overview.lowStockCount || 0}
            icon="⚠️"
            color="text-yellow-600"
          />
          <StatCard
            label="Out of Stock"
            value={overview.outOfStockCount || 0}
            icon="❌"
            color="text-red-600"
          />
          <StatCard
            label="Active Products"
            value={overview.totalProducts || 0}
            icon="✅"
            color="text-green-600"
          />
        </div>
      </div>

      {/* Inventory Alerts */}
      {(overview.lowStockProducts?.length > 0 || overview.outOfStockProducts?.length > 0) && (
        <div>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">🚨 Inventory Alerts</h3>
          
          {/* Low Stock Alert */}
          {overview.lowStockProducts?.length > 0 && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4">
              <div className="flex items-center mb-2">
                <span className="text-yellow-600 text-lg mr-2">⚠️</span>
                <h4 className="text-yellow-800 font-medium">Low Stock Alert</h4>
              </div>
              <p className="text-yellow-700 text-sm mb-3">
                The following products have low stock (less than 5 units):
              </p>
              <div className="space-y-2">
                {overview.lowStockProducts.map(product => (
                  <div key={product.id} className="flex justify-between items-center bg-white p-2 rounded border">
                    <span className="text-sm font-medium">{product.name}</span>
                    <span className="text-sm text-red-600 font-bold">
                      Only {product.stock} left
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
          
          {/* Out of Stock Alert */}
          {overview.outOfStockProducts?.length > 0 && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <div className="flex items-center mb-2">
                <span className="text-red-600 text-lg mr-2">❌</span>
                <h4 className="text-red-800 font-medium">Out of Stock Alert</h4>
              </div>
              <p className="text-red-700 text-sm mb-3">
                The following products are completely out of stock:
              </p>
              <div className="space-y-2">
                {overview.outOfStockProducts.map(product => (
                  <div key={product.id} className="flex justify-between items-center bg-white p-2 rounded border">
                    <span className="text-sm font-medium">{product.name}</span>
                    <span className="text-sm text-red-600 font-bold">
                      Out of Stock
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Activity */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-4">📋 Recent Activity</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <StatCard
            label="New Registrations (Last 7 Days)"
            value={overview.recentRegistrationsCount || 0}
            icon="📝"
            color="text-blue-600"
          />
          <StatCard
            label="Recent Orders (Last 7 Days)"
            value={overview.recentOrdersCount || 0}
            icon="📊"
            color="text-indigo-600"
          />
        </div>
      </div>
    </div>
  );
}
