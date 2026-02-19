import React, { useEffect, useState } from 'react';
import { Row, Col, Button, Badge, Table, Alert, Card, CardBody, Input, Modal, ModalHeader, ModalBody, ModalFooter } from 'reactstrap';
import Widget from '../components/Widget/Widget';
import { collection, getDocs, doc, updateDoc, query, orderBy, Timestamp } from 'firebase/firestore';
import { db } from '../lib/firebase';

interface Review {
  id: string;
  orderId: string;
  userId: string;
  userName: string;
  rating: number;
  comment: string;
  createdAt: Timestamp;
  menuItemIds: string[];
  isEnabled: boolean;
  moderationStatus: 'approved' | 'pending' | 'rejected';
}

const ReviewsPage: React.FC = () => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<'all' | 'enabled' | 'disabled'>('all');
  const [selectedReview, setSelectedReview] = useState<Review | null>(null);
  const [detailsModal, setDetailsModal] = useState(false);

  useEffect(() => {
    fetchReviews();
  }, []);

  const fetchReviews = async () => {
    try {
      setLoading(true);
      const reviewsRef = collection(db, 'reviews');
      const q = query(reviewsRef, orderBy('createdAt', 'desc'));
      const snapshot = await getDocs(q);
      
      const reviewsData: Review[] = [];
      snapshot.forEach((doc) => {
        reviewsData.push({ id: doc.id, ...doc.data() } as Review);
      });
      
      setReviews(reviewsData);
      setError('');
    } catch (err) {
      console.error('Error fetching reviews:', err);
      setError('Failed to fetch reviews. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleEnable = async (reviewId: string, currentStatus: boolean) => {
    try {
      const reviewRef = doc(db, 'reviews', reviewId);
      await updateDoc(reviewRef, {
        isEnabled: !currentStatus
      });
      
      setSuccess(`Review ${!currentStatus ? 'enabled' : 'disabled'} successfully!`);
      setTimeout(() => setSuccess(''), 3000);
      
      // Update local state
      setReviews(reviews.map(r => 
        r.id === reviewId ? { ...r, isEnabled: !currentStatus } : r
      ));
    } catch (err) {
      console.error('Error updating review:', err);
      setError('Failed to update review. Please try again.');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleUpdateModerationStatus = async (reviewId: string, status: 'approved' | 'pending' | 'rejected') => {
    try {
      const reviewRef = doc(db, 'reviews', reviewId);
      await updateDoc(reviewRef, {
        moderationStatus: status
      });
      
      setSuccess(`Review marked as ${status}!`);
      setTimeout(() => setSuccess(''), 3000);
      
      // Update local state
      setReviews(reviews.map(r => 
        r.id === reviewId ? { ...r, moderationStatus: status } : r
      ));
    } catch (err) {
      console.error('Error updating moderation status:', err);
      setError('Failed to update moderation status. Please try again.');
      setTimeout(() => setError(''), 3000);
    }
  };

  const renderStars = (rating: number) => {
    return (
      <span style={{ color: '#FFB300', fontSize: '18px' }}>
        {Array.from({ length: 5 }, (_, i) => (
          <span key={i}>{i < rating ? '★' : '☆'}</span>
        ))}
      </span>
    );
  };

  const filteredReviews = reviews.filter(review => {
    // Filter by status
    if (filterStatus === 'enabled' && !review.isEnabled) return false;
    if (filterStatus === 'disabled' && review.isEnabled) return false;
    
    // Filter by search query
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      return (
        review.userName.toLowerCase().includes(query) ||
        review.comment.toLowerCase().includes(query) ||
        review.orderId.toLowerCase().includes(query)
      );
    }
    
    return true;
  });

  const stats = {
    total: reviews.length,
    enabled: reviews.filter(r => r.isEnabled).length,
    disabled: reviews.filter(r => !r.isEnabled).length,
    avgRating: reviews.length > 0 
      ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1)
      : '0'
  };

  return (
    <div>
      <Row>
        <Col>
          <h1 className="page-title" style={{ marginBottom: '30px' }}>
            Reviews Management 🌟
          </h1>
        </Col>
      </Row>

      {error && <Alert color="danger">{error}</Alert>}
      {success && <Alert color="success">{success}</Alert>}

      {/* Stats Cards */}
      <Row>
        <Col lg={3} md={6} sm={6} xs={12}>
          <Widget
            title={<h5>Total Reviews</h5>}
          >
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <h2 className="mb-0">{stats.total}</h2>
                <p className="text-muted mb-0">All time</p>
              </div>
              <i className="la la-comment la-3x text-primary" />
            </div>
          </Widget>
        </Col>
        <Col lg={3} md={6} sm={6} xs={12}>
          <Widget
            title={<h5>Enabled</h5>}
          >
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <h2 className="mb-0 text-success">{stats.enabled}</h2>
                <p className="text-muted mb-0">Visible to users</p>
              </div>
              <i className="la la-check-circle la-3x text-success" />
            </div>
          </Widget>
        </Col>
        <Col lg={3} md={6} sm={6} xs={12}>
          <Widget
            title={<h5>Disabled</h5>}
          >
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <h2 className="mb-0 text-danger">{stats.disabled}</h2>
                <p className="text-muted mb-0">Hidden from users</p>
              </div>
              <i className="la la-ban la-3x text-danger" />
            </div>
          </Widget>
        </Col>
        <Col lg={3} md={6} sm={6} xs={12}>
          <Widget
            title={<h5>Average Rating</h5>}
          >
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <h2 className="mb-0 text-warning">{stats.avgRating} ⭐</h2>
                <p className="text-muted mb-0">Overall rating</p>
              </div>
              <i className="la la-star la-3x text-warning" />
            </div>
          </Widget>
        </Col>
      </Row>

      {/* Filters */}
      <Row className="mb-4">
        <Col md={6}>
          <Input
            type="text"
            placeholder="Search by user name, comment, or order ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </Col>
        <Col md={6}>
          <div className="d-flex gap-2">
            <Button
              color={filterStatus === 'all' ? 'primary' : 'secondary'}
              onClick={() => setFilterStatus('all')}
              style={{ flex: 1 }}
            >
              All ({stats.total})
            </Button>
            <Button
              color={filterStatus === 'enabled' ? 'success' : 'secondary'}
              onClick={() => setFilterStatus('enabled')}
              style={{ flex: 1 }}
            >
              Enabled ({stats.enabled})
            </Button>
            <Button
              color={filterStatus === 'disabled' ? 'danger' : 'secondary'}
              onClick={() => setFilterStatus('disabled')}
              style={{ flex: 1 }}
            >
              Disabled ({stats.disabled})
            </Button>
          </div>
        </Col>
      </Row>

      {/* Reviews Table */}
      <Widget
        title={<h5>All Reviews ({filteredReviews.length})</h5>}
      >
        {loading ? (
          <div className="text-center py-4">
            <div className="spinner-border text-primary" role="status">
              <span className="sr-only">Loading...</span>
            </div>
          </div>
        ) : filteredReviews.length === 0 ? (
          <div className="text-center py-4">
            <p className="text-muted">No reviews found.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <Table className="table-hover mb-0">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>User</th>
                  <th>Rating</th>
                  <th>Comment</th>
                  <th>Order ID</th>
                  <th>Status</th>
                  <th>Moderation</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredReviews.map((review) => (
                  <tr key={review.id}>
                    <td style={{ fontSize: '13px' }}>
                      {review.createdAt?.toDate?.()?.toLocaleDateString('en-US', {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </td>
                    <td>
                      <strong>{review.userName || 'Anonymous'}</strong>
                      <br />
                      <small className="text-muted">{review.userId.substring(0, 8)}...</small>
                    </td>
                    <td>{renderStars(review.rating)}</td>
                    <td style={{ maxWidth: '300px' }}>
                      <div style={{
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical'
                      }}>
                        {review.comment || <em className="text-muted">No comment</em>}
                      </div>
                    </td>
                    <td>
                      <code style={{ fontSize: '12px' }}>
                        {review.orderId.substring(0, 8)}...
                      </code>
                    </td>
                    <td>
                      {review.isEnabled ? (
                        <Badge color="success">Enabled</Badge>
                      ) : (
                        <Badge color="danger">Disabled</Badge>
                      )}
                    </td>
                    <td>
                      <Badge 
                        color={
                          review.moderationStatus === 'approved' ? 'success' :
                          review.moderationStatus === 'pending' ? 'warning' :
                          'danger'
                        }
                      >
                        {review.moderationStatus}
                      </Badge>
                    </td>
                    <td>
                      <div className="d-flex gap-1">
                        <Button
                          size="sm"
                          color="info"
                          onClick={() => {
                            setSelectedReview(review);
                            setDetailsModal(true);
                          }}
                        >
                          <i className="la la-eye" />
                        </Button>
                        <Button
                          size="sm"
                          color={review.isEnabled ? 'danger' : 'success'}
                          onClick={() => handleToggleEnable(review.id, review.isEnabled)}
                        >
                          <i className={review.isEnabled ? 'la la-ban' : 'la la-check'} />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </div>
        )}
      </Widget>

      {/* Details Modal */}
      <Modal isOpen={detailsModal} toggle={() => setDetailsModal(false)} size="lg">
        <ModalHeader toggle={() => setDetailsModal(false)}>
          Review Details
        </ModalHeader>
        <ModalBody>
          {selectedReview && (
            <div>
              <Card className="mb-3">
                <CardBody>
                  <h5>User Information</h5>
                  <p><strong>Name:</strong> {selectedReview.userName}</p>
                  <p><strong>User ID:</strong> <code>{selectedReview.userId}</code></p>
                  <p><strong>Order ID:</strong> <code>{selectedReview.orderId}</code></p>
                  <p><strong>Date:</strong> {selectedReview.createdAt?.toDate?.()?.toLocaleString()}</p>
                </CardBody>
              </Card>

              <Card className="mb-3">
                <CardBody>
                  <h5>Review Content</h5>
                  <div className="mb-3">
                    <strong>Rating:</strong> {renderStars(selectedReview.rating)} ({selectedReview.rating}/5)
                  </div>
                  <div>
                    <strong>Comment:</strong>
                    <p className="mt-2" style={{ 
                      padding: '15px', 
                      background: '#f8f9fa', 
                      borderRadius: '8px',
                      whiteSpace: 'pre-wrap'
                    }}>
                      {selectedReview.comment || <em className="text-muted">No comment provided</em>}
                    </p>
                  </div>
                </CardBody>
              </Card>

              <Card>
                <CardBody>
                  <h5>Moderation</h5>
                  <div className="d-flex gap-2 mb-3">
                    <Button
                      color={selectedReview.moderationStatus === 'approved' ? 'success' : 'outline-success'}
                      onClick={() => handleUpdateModerationStatus(selectedReview.id, 'approved')}
                    >
                      ✓ Approve
                    </Button>
                    <Button
                      color={selectedReview.moderationStatus === 'pending' ? 'warning' : 'outline-warning'}
                      onClick={() => handleUpdateModerationStatus(selectedReview.id, 'pending')}
                    >
                      ⏳ Pending
                    </Button>
                    <Button
                      color={selectedReview.moderationStatus === 'rejected' ? 'danger' : 'outline-danger'}
                      onClick={() => handleUpdateModerationStatus(selectedReview.id, 'rejected')}
                    >
                      ✗ Reject
                    </Button>
                  </div>
                  <div>
                    <strong>Current Status:</strong>{' '}
                    <Badge color={selectedReview.isEnabled ? 'success' : 'danger'}>
                      {selectedReview.isEnabled ? 'Enabled' : 'Disabled'}
                    </Badge>
                  </div>
                </CardBody>
              </Card>
            </div>
          )}
        </ModalBody>
        <ModalFooter>
          <Button color="secondary" onClick={() => setDetailsModal(false)}>
            Close
          </Button>
        </ModalFooter>
      </Modal>
    </div>
  );
};

export default ReviewsPage;
