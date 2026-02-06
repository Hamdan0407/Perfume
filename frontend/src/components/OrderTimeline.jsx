import React, { useState, useEffect } from 'react';
import { CheckCircle, Clock, Truck, Package, XCircle, RotateCcw } from 'lucide-react';
import api from '../../api/axios';

const OrderTimeline = ({ orderId }) => {
  const [timeline, setTimeline] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchTimeline();
  }, [orderId]);

  const fetchTimeline = async () => {
    try {
      setLoading(true);
      const { data } = await api.get(`/orders/${orderId}/timeline`);
      setTimeline(data);
    } catch (err) {
      setError('Failed to load order timeline');
      console.error('Timeline fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusIcon = (status) => {
    const statusLower = status.toLowerCase();
    const iconProps = { size: 20 };

    switch (statusLower) {
      case 'placed':
        return <CheckCircle {...iconProps} className="text-green-500" />;
      case 'confirmed':
        return <CheckCircle {...iconProps} className="text-blue-500" />;
      case 'packed':
        return <Package {...iconProps} className="text-yellow-500" />;
      case 'shipped':
        return <Truck {...iconProps} className="text-purple-500" />;
      case 'delivered':
        return <CheckCircle {...iconProps} className="text-green-600" />;
      case 'cancelled':
        return <XCircle {...iconProps} className="text-red-500" />;
      case 'refunded':
        return <RotateCcw {...iconProps} className="text-orange-500" />;
      default:
        return <Clock {...iconProps} className="text-gray-500" />;
    }
  };

  const getStatusColor = (status, isActive) => {
    if (isActive) return 'border-blue-500 bg-blue-50';

    const statusLower = status.toLowerCase();
    switch (statusLower) {
      case 'placed':
      case 'confirmed':
      case 'delivered':
        return 'border-green-500 bg-green-50';
      case 'cancelled':
      case 'refunded':
        return 'border-red-500 bg-red-50';
      default:
        return 'border-gray-300 bg-gray-50';
    }
  };

  const formatDateTime = (dateTimeString) => {
    const date = new Date(dateTimeString);
    return {
      date: date.toLocaleDateString(),
      time: date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-8">
        <p className="text-red-600">{error}</p>
      </div>
    );
  }

  if (timeline.length === 0) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-600">No timeline data available</p>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h3 className="text-lg font-semibold mb-6 text-center">Order Timeline</h3>

      <div className="space-y-4">
        {timeline.map((item, index) => {
          const { date, time } = formatDateTime(item.timestamp);
          const isLast = index === timeline.length - 1;

          return (
            <div key={item.id} className="flex items-start space-x-4">
              {/* Timeline line */}
              <div className="flex flex-col items-center">
                <div className={`w-10 h-10 rounded-full border-2 flex items-center justify-center ${getStatusColor(item.status, item.isActive)}`}>
                  {getStatusIcon(item.status)}
                </div>
                {!isLast && (
                  <div className="w-0.5 h-16 bg-gray-300 mt-2"></div>
                )}
              </div>

              {/* Content */}
              <div className="flex-grow pb-8">
                <div className="bg-white rounded-lg shadow-sm border p-4">
                  <div className="flex justify-between items-start mb-2">
                    <h4 className="font-medium text-gray-900 capitalize">
                      {item.status.toLowerCase().replace('_', ' ')}
                    </h4>
                    {item.isActive && (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        Current Status
                      </span>
                    )}
                  </div>

                  <div className="text-sm text-gray-600 mb-2">
                    <div className="flex items-center space-x-4">
                      <span>{date}</span>
                      <span>{time}</span>
                    </div>
                  </div>

                  {item.updatedBy && item.updatedBy !== 'SYSTEM' && (
                    <div className="text-sm text-gray-500 mb-2">
                      Updated by: {item.updatedBy}
                    </div>
                  )}

                  {item.notes && (
                    <div className="text-sm text-gray-700 bg-gray-50 rounded p-2">
                      {item.notes}
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default OrderTimeline;