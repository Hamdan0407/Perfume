import React, { useState, useEffect } from 'react';
import { Eye, CheckCircle, Clock, X as XIcon, Edit, Save, X } from 'lucide-react';
import { toast } from 'react-toastify';
import '../styles/AdminOrders.css';

export default function AdminOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('All');
  const [editingOrder, setEditingOrder] = useState(null);
  const [editForm, setEditForm] = useState({ status: '', notes: '' });

  useEffect(() => {
    // Demo mode: Use mock orders data
    setOrders([
      {
        id: 'ORD-001',
        customerName: 'John Doe',
        email: 'john@example.com',
        status: 'Delivered',
        total: 299.99,
        date: '2024-01-15',
        items: 3
      },
      {
        id: 'ORD-002',
        customerName: 'Jane Smith',
        email: 'jane@example.com',
        status: 'Processing',
        total: 189.50,
        date: '2024-01-20',
        items: 2
      }
    ]);
    setLoading(false);
  }, []);

  const fetchOrders = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/admin/orders?size=50', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      if (response.ok) {
        const data = await response.json();
        setOrders(data.content || []);
      } else {
        // Gracefully handle auth errors - show empty list
        console.warn('Failed to load admin orders, showing empty list');
        setOrders([]);
      }
    } catch (error) {
      console.error('Error fetching orders:', error);
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  const filteredOrders = filter === 'All' 
    ? orders 
    : orders.filter(o => {
        const status = o.status?.toUpperCase() || '';
        return status === filter.toUpperCase();
      });

  const getStatusIcon = (status) => {
    const statusUpper = status?.toUpperCase() || '';
    if (['PENDING', 'PROCESSING'].includes(statusUpper)) return <Clock size={16} />;
    if (['SHIPPED', 'DELIVERED'].includes(statusUpper)) return <CheckCircle size={16} />;
    if (statusUpper === 'CANCELLED') return <XIcon size={16} />;
    return null;
  };

  const getStatusDisplay = (status) => {
    const statusMap = {
      'PENDING': 'Pending',
      'PROCESSING': 'Processing',
      'PLACED': 'Placed',
      'CONFIRMED': 'Confirmed',
      'PACKED': 'Packed',
      'SHIPPED': 'Shipped',
      'DELIVERED': 'Delivered',
      'CANCELLED': 'Cancelled',
      'REFUNDED': 'Refunded'
    };
    return statusMap[status?.toUpperCase()] || status || 'Unknown';
  };

  const handleEditStatus = (order) => {
    setEditingOrder(order.id);
    setEditForm({
      status: order.status || 'PLACED',
      notes: ''
    });
  };

  const handleCancelEdit = () => {
    setEditingOrder(null);
    setEditForm({ status: '', notes: '' });
  };

  const handleSaveStatus = async () => {
    if (!editForm.status) {
      toast.error('Please select a status');
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/api/admin/orders/${editingOrder}/status`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify(editForm)
      });

      if (response.ok) {
        const updatedOrder = await response.json();
        setOrders(orders.map(order =>
          order.id === editingOrder ? updatedOrder : order
        ));
        toast.success('Order status updated successfully');
        setEditingOrder(null);
        setEditForm({ status: '', notes: '' });
      } else {
        const error = await response.text();
        toast.error(`Failed to update status: ${error}`);
      }
    } catch (error) {
      console.error('Error updating order status:', error);
      toast.error('Failed to update order status');
    }
  };

  return (
    <div className="orders-container">
      <div className="orders-header">
        <h2>Orders Management</h2>
        <div className="order-filters">
          {['All', 'Pending', 'Processing', 'Shipped', 'Delivered'].map(status => (
            <button
              key={status}
              className={`filter-btn ${filter === status ? 'active' : ''}`}
              onClick={() => setFilter(status)}
            >
              {status}
            </button>
          ))}
        </div>
      </div>

      <div className="orders-table-wrapper">
        <table className="orders-table">
          <thead>
            <tr>
              <th>Order Number</th>
              <th>Customer</th>
              <th>Items</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Date</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="7" className="loading">Loading...</td></tr>
            ) : filteredOrders.length === 0 ? (
              <tr><td colSpan="7" className="empty">No orders found</td></tr>
            ) : (
              filteredOrders.map(order => (
                <tr key={order.id}>
                  <td><strong>{order.orderNumber || `#ORD-${order.id}`}</strong></td>
                  <td>{order.user?.firstName} {order.user?.lastName || 'N/A'}</td>
                  <td>{order.items?.length || 0}</td>
                  <td className="amount">₹{order.totalAmount?.toFixed(2) || '0.00'}</td>
                  <td>
                    {editingOrder === order.id ? (
                      <div className="status-edit-container">
                        <select
                          value={editForm.status}
                          onChange={(e) => setEditForm({...editForm, status: e.target.value})}
                          className="status-select"
                        >
                          <option value="PLACED">Placed</option>
                          <option value="CONFIRMED">Confirmed</option>
                          <option value="PACKED">Packed</option>
                          <option value="SHIPPED">Shipped</option>
                          <option value="DELIVERED">Delivered</option>
                          <option value="CANCELLED">Cancelled</option>
                          <option value="REFUNDED">Refunded</option>
                        </select>
                        <textarea
                          value={editForm.notes}
                          onChange={(e) => setEditForm({...editForm, notes: e.target.value})}
                          placeholder="Add notes (optional)"
                          className="notes-textarea"
                          rows="2"
                        />
                      </div>
                    ) : (
                      <span className={`status-badge ${order.status?.toLowerCase()}`}>
                        {getStatusIcon(order.status)}
                        {getStatusDisplay(order.status)}
                      </span>
                    )}
                  </td>
                  <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                  <td>
                    {editingOrder === order.id ? (
                      <div className="edit-actions">
                        <button
                          className="btn-save"
                          onClick={handleSaveStatus}
                          title="Save Changes"
                        >
                          <Save size={16} />
                        </button>
                        <button
                          className="btn-cancel"
                          onClick={handleCancelEdit}
                          title="Cancel Edit"
                        >
                          <X size={16} />
                        </button>
                      </div>
                    ) : (
                      <div className="action-buttons">
                        <button className="btn-view" title="View Details">
                          <Eye size={16} />
                        </button>
                        <button
                          className="btn-edit"
                          onClick={() => handleEditStatus(order)}
                          title="Edit Status"
                        >
                          <Edit size={16} />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
