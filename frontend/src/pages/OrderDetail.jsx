import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../api/axios';
import OrderTimeline from '../components/OrderTimeline';

export default function OrderDetail() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchOrder();
  }, [id]);

  const fetchOrder = async () => {
    try {
      const { data } = await api.get(`/orders/${id}`);
      setOrder(data);
    } catch (error) {
      toast.error('Failed to load order');
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadInvoice = async () => {
    console.log('Download Invoice button clicked');
    try {
      const response = await api.get(`/orders/${id}/invoice`, {
        responseType: 'blob'
      });
      console.log('Invoice response:', response);

      // Check if response is a PDF
      const contentType = response.headers['content-type'];
      if (!contentType || !contentType.includes('pdf')) {
        toast.error('Invoice not available or backend error');
        return;
      }

      // Create blob and download
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Invoice_${order.orderNumber}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Invoice downloaded successfully');
    } catch (error) {
      console.error('Download invoice error:', error);
      toast.error('Could not download invoice');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-600 text-lg">Order not found.</p>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold mb-8">Order Details</h1>

      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="grid grid-cols-2 gap-6">
          <div>
            <h3 className="font-semibold mb-2">Order Information</h3>
            <p className="text-sm text-gray-600">Order Number: <span className="font-medium">{order.orderNumber}</span></p>
            <p className="text-sm text-gray-600">Date: {new Date(order.createdAt).toLocaleDateString()}</p>
            <p className="text-sm text-gray-600">Status: <span className="font-medium">{order.status}</span></p>
          </div>
          <div>
            <h3 className="font-semibold mb-2">Shipping Address</h3>
            <p className="text-sm text-gray-600">{order.shippingAddress}</p>
            <p className="text-sm text-gray-600">{order.shippingCity}, {order.shippingCountry}</p>
            <p className="text-sm text-gray-600">{order.shippingZipCode}</p>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="font-semibold mb-4">Order Items</h3>
        <div className="space-y-4">
          {order.items.map((item) => (
            <div key={item.id} className="flex items-center space-x-4 border-b pb-4">
              <img
                src={item.product.imageUrl}
                alt={item.product.name}
                className="w-20 h-20 object-cover rounded"
              />
              <div className="flex-grow">
                <p className="font-medium">{item.product.name}</p>
                <p className="text-sm text-gray-600">Quantity: {item.quantity}</p>
              </div>
              <p className="font-semibold">${item.subtotal.toFixed(2)}</p>
            </div>
          ))}
        </div>

        <div className="mt-6 border-t pt-4 space-y-2">
          <div className="flex justify-between">
            <span className="text-gray-600">Subtotal</span>
            <span>${order.subtotal.toFixed(2)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">Tax</span>
            <span>${order.tax.toFixed(2)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">Shipping</span>
            <span>${order.shippingCost.toFixed(2)}</span>
          </div>
          <div className="flex justify-between text-xl font-bold border-t pt-2">
            <span>Total</span>
            <span className="text-primary-600">₹{order.totalAmount.toFixed(2)}</span>
          </div>

          {/* Show Download Invoice button if order is not cancelled */}
          {order.status !== 'CANCELLED' && (
            <button
              className="mt-4 w-full px-4 py-2 bg-primary-600 text-white rounded shadow hover:bg-primary-700 transition"
              onClick={handleDownloadInvoice}
            >
              Download Invoice
            </button>
          )}
        </div>

        {/* Order Timeline */}
        <div className="bg-white rounded-lg shadow p-6 mt-6">
          <OrderTimeline orderId={id} />
        </div>
      </div>
    </div>
  );
}
