import React, { useEffect, useState } from 'react';
import { Row, Col, Alert, Modal, ModalHeader, ModalBody, ModalFooter, Button } from 'reactstrap';
import { collection, getDocs, doc, updateDoc, query, orderBy, Timestamp } from 'firebase/firestore';
import { db } from '../lib/firebase';
import Swal from 'sweetalert2';
import { showToast } from '../utils/toast';
import { useGSAP } from '@gsap/react';
import gsap from 'gsap';
import UniformLoader from '../components/UniformLoader/UniformLoader';

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

// ─── micro helpers ────────────────────────────────────────────────────────────

const StarRating: React.FC<{ rating: number; size?: number }> = ({ rating, size = 16 }) => (
  <span style={{ display: 'inline-flex', gap: 2 }}>
    {Array.from({ length: 5 }, (_, i) => (
      <svg key={i} width={size} height={size} viewBox="0 0 24 24"
        fill={i < rating ? '#F59E0B' : 'none'}
        stroke={i < rating ? '#F59E0B' : '#D1D5DB'} strokeWidth="1.5">
        <path strokeLinecap="round" strokeLinejoin="round"
          d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.563.563 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
      </svg>
    ))}
  </span>
);

const UserAvatar: React.FC<{ name: string }> = ({ name }) => {
  const initials = name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase() || '?';
  const colors = ['#6366F1', '#8B5CF6', '#EC4899', '#14B8A6', '#F97316', '#0EA5E9'];
  const color = colors[name.charCodeAt(0) % colors.length];
  return (
    <div style={{
      width: 36, height: 36, borderRadius: '50%', background: color,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      color: '#fff', fontWeight: 700, fontSize: 13, flexShrink: 0, userSelect: 'none'
    }}>
      {initials}
    </div>
  );
};

const ModerationBadge: React.FC<{ status: string }> = ({ status }) => {
  const cfg: Record<string, { bg: string; color: string; label: string }> = {
    approved: { bg: '#DCFCE7', color: '#16A34A', label: '✓ Approved' },
    pending:  { bg: '#FEF9C3', color: '#B45309', label: '⧖ Pending'  },
    rejected: { bg: '#FEE2E2', color: '#DC2626', label: '✕ Rejected' },
  };
  const c = cfg[status] ?? { bg: '#F3F4F6', color: '#6B7280', label: status };
  return (
    <span style={{
      background: c.bg, color: c.color, borderRadius: 20,
      padding: '4px 12px', fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap',
      display: 'inline-block'
    }}>
      {c.label}
    </span>
  );
};

const VisibilityBadge: React.FC<{ enabled: boolean }> = ({ enabled }) => (
  <span style={{
    background: enabled ? '#DCFCE7' : '#FEE2E2',
    color: enabled ? '#16A34A' : '#DC2626',
    borderRadius: 20, padding: '4px 12px', fontSize: 12, fontWeight: 600,
    display: 'inline-block', whiteSpace: 'nowrap'
  }}>
    {enabled ? '● Visible' : '● Hidden'}
  </span>
);

// reusable small action button
const ActionBtn: React.FC<{
  label: string;
  onClick: () => void;
  variant: 'neutral' | 'green' | 'red' | 'blue';
}> = ({ label, onClick, variant }) => {
  const v = {
    neutral: { bg: '#F9FAFB', color: '#374151', border: '#E5E7EB' },
    green:   { bg: '#F0FDF4', color: '#16A34A', border: '#BBF7D0' },
    red:     { bg: '#FEF2F2', color: '#DC2626', border: '#FECACA' },
    blue:    { bg: '#EEF2FF', color: '#6366F1', border: '#C7D2FE' },
  }[variant];
  return (
    <button
      onClick={onClick}
      style={{
        padding: '5px 11px', borderRadius: 7, fontSize: 12, fontWeight: 600,
        border: `1.5px solid ${v.border}`, background: v.bg, color: v.color,
        cursor: 'pointer', whiteSpace: 'nowrap', lineHeight: 1.4,
        transition: 'opacity .15s',
      }}
      onMouseEnter={e => (e.currentTarget.style.opacity = '.75')}
      onMouseLeave={e => (e.currentTarget.style.opacity = '1')}
    >
      {label}
    </button>
  );
};

// ─── component ────────────────────────────────────────────────────────────────

const ReviewsPage: React.FC = () => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [moderationFilter, setModerationFilter] = useState<'all' | 'pending' | 'approved' | 'rejected'>('all');
  const [selectedReview, setSelectedReview] = useState<Review | null>(null);
  const [detailsModal, setDetailsModal] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  useEffect(() => { fetchReviews(); }, []);

  const fetchReviews = async () => {
    try {
      setLoading(true);
      const q = query(collection(db, 'reviews'), orderBy('createdAt', 'desc'));
      const snapshot = await getDocs(q);
      setReviews(snapshot.docs.map(d => ({ id: d.id, ...d.data() } as Review)));
      setError('');
    } catch (err) {
      console.error('Error fetching reviews:', err);
      setError('Failed to fetch reviews. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleEnable = async (reviewId: string, currentStatus: boolean) => {
    const action = currentStatus ? 'disable' : 'enable';
    const result = await Swal.fire({
      title: `${currentStatus ? 'Hide' : 'Show'} Review?`,
      text: `This review will be ${currentStatus ? 'hidden from' : 'visible to'} app users.`,
      icon: 'question', showCancelButton: true,
      confirmButtonColor: currentStatus ? '#DC2626' : '#16A34A',
      cancelButtonColor: '#6B7280',
      confirmButtonText: currentStatus ? 'Yes, Hide' : 'Yes, Show',
      cancelButtonText: 'Cancel',
    });
    if (result.isConfirmed) {
      try {
        await updateDoc(doc(db, 'reviews', reviewId), { isEnabled: !currentStatus });
        showToast('success', `Review ${action}d!`);
        setReviews(prev => prev.map(r => r.id === reviewId ? { ...r, isEnabled: !currentStatus } : r));
        if (selectedReview?.id === reviewId) setSelectedReview(r => r ? { ...r, isEnabled: !currentStatus } : r);
      } catch { showToast('error', 'Failed to update review'); }
    }
  };

  const handleUpdateModerationStatus = async (reviewId: string, status: 'approved' | 'pending' | 'rejected') => {
    const cfg = {
      approved: { title: 'Approve Review?', btn: 'Approve', color: '#16A34A' },
      pending:  { title: 'Mark as Pending?', btn: 'Mark Pending', color: '#B45309' },
      rejected: { title: 'Reject Review?',  btn: 'Reject',  color: '#DC2626' },
    };
    const c = cfg[status];
    const result = await Swal.fire({
      title: c.title, text: `Mark this review as "${status}"?`,
      icon: 'question', showCancelButton: true,
      confirmButtonColor: c.color, cancelButtonColor: '#6B7280',
      confirmButtonText: c.btn, cancelButtonText: 'Cancel',
    });
    if (result.isConfirmed) {
      try {
        await updateDoc(doc(db, 'reviews', reviewId), { moderationStatus: status });
        showToast('success', `Review marked as ${status}!`);
        setReviews(prev => prev.map(r => r.id === reviewId ? { ...r, moderationStatus: status } : r));
        if (selectedReview?.id === reviewId) setSelectedReview(r => r ? { ...r, moderationStatus: status } : r);
      } catch { showToast('error', 'Failed to update moderation status'); }
    }
  };

  const filteredReviews = reviews.filter(review => {
    if (moderationFilter !== 'all' && review.moderationStatus !== moderationFilter) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return review.userName.toLowerCase().includes(q) ||
             review.comment.toLowerCase().includes(q) ||
             review.orderId.toLowerCase().includes(q);
    }
    return true;
  });

  useEffect(() => { setCurrentPage(1); }, [searchQuery, moderationFilter]);

  const totalPages = Math.ceil(filteredReviews.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const paginatedReviews = filteredReviews.slice(startIndex, startIndex + itemsPerPage);

  useGSAP(() => {
    if (paginatedReviews.length > 0)
      gsap.from('.review-row', { opacity: 0, y: 14, duration: 0.22, stagger: 0.04, ease: 'power2.out' });
  }, [currentPage, paginatedReviews.length]);

  const stats = {
    total:     reviews.length,
    pending:   reviews.filter(r => r.moderationStatus === 'pending').length,
    approved:  reviews.filter(r => r.moderationStatus === 'approved').length,
    rejected:  reviews.filter(r => r.moderationStatus === 'rejected').length,
    avgRating: reviews.length > 0
      ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1)
      : '0.0',
  };

  const filterTabs: { key: typeof moderationFilter; label: string; count: number; active: string }[] = [
    { key: 'all',      label: 'All',      count: stats.total,    active: '#6366F1' },
    { key: 'pending',  label: 'Pending',  count: stats.pending,  active: '#B45309' },
    { key: 'approved', label: 'Approved', count: stats.approved, active: '#16A34A' },
    { key: 'rejected', label: 'Rejected', count: stats.rejected, active: '#DC2626' },
  ];

  // ── stat card definitions (SVG emoji replacements for icons) ────────────────
  const statCards = [
    { label: 'Total Reviews', value: stats.total,           emoji: '💬', bg: '#EEF2FF' },
    { label: 'Pending',       value: stats.pending,         emoji: '⏳', bg: '#FEF9C3' },
    { label: 'Approved',      value: stats.approved,        emoji: '✅', bg: '#DCFCE7' },
    { label: 'Avg Rating',    value: `${stats.avgRating} ★`, emoji: '⭐', bg: '#FFF7ED' },
  ];

  return (
    <div>
      {/* ── header ── */}
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h1 className="page-title mb-1">Reviews</h1>
          <p className="text-muted mb-0" style={{ fontSize: 14 }}>Manage and moderate customer feedback</p>
        </div>
        <button
          onClick={fetchReviews}
          style={{
            padding: '7px 16px', borderRadius: 8, fontSize: 13, fontWeight: 600,
            border: '1.5px solid #E5E7EB', background: '#fff', color: '#374151',
            cursor: 'pointer'
          }}
        >
          ↺ Refresh
        </button>
      </div>

      {error && <Alert color="danger">{error}</Alert>}

      {/* ── stat cards ── */}
      <Row className="mb-4 g-3">
        {statCards.map(card => (
          <Col key={card.label} lg={3} md={6} xs={12}>
            <div style={{
              background: '#fff', borderRadius: 12, padding: '18px 20px',
              boxShadow: '0 1px 4px rgba(0,0,0,.07)', display: 'flex',
              alignItems: 'center', gap: 14
            }}>
              <div style={{
                width: 48, height: 48, borderRadius: 12, background: card.bg,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 22, flexShrink: 0
              }}>
                {card.emoji}
              </div>
              <div>
                <div style={{ fontSize: 24, fontWeight: 700, lineHeight: 1.1 }}>{card.value}</div>
                <div style={{ fontSize: 13, color: '#6B7280', marginTop: 3 }}>{card.label}</div>
              </div>
            </div>
          </Col>
        ))}
      </Row>

      {/* ── main card ── */}
      <div style={{ background: '#fff', borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,.07)', overflow: 'hidden' }}>

        {/* toolbar */}
        <div style={{ padding: '16px 20px', borderBottom: '1px solid #F3F4F6' }}>
          <div className="d-flex flex-wrap align-items-center gap-3">
            {/* search */}
            <input
              type="text"
              placeholder="🔍  Search by user, comment, or order ID…"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              style={{
                flex: '1 1 220px', minWidth: 180, padding: '8px 14px',
                border: '1.5px solid #E5E7EB', borderRadius: 8, fontSize: 14, outline: 'none'
              }}
            />
            {/* filter tabs */}
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {filterTabs.map(tab => {
                const on = moderationFilter === tab.key;
                return (
                  <button
                    key={tab.key}
                    onClick={() => setModerationFilter(tab.key)}
                    style={{
                      padding: '7px 14px', borderRadius: 20, fontSize: 13, fontWeight: 600,
                      border: `1.5px solid ${on ? tab.active : '#E5E7EB'}`,
                      background: on ? tab.active : '#fff',
                      color: on ? '#fff' : '#6B7280',
                      cursor: 'pointer', transition: 'all .15s', whiteSpace: 'nowrap'
                    }}
                  >
                    {tab.label}&nbsp;
                    <span style={{
                      background: on ? 'rgba(255,255,255,.25)' : '#F3F4F6',
                      color: on ? '#fff' : '#6B7280',
                      borderRadius: 10, padding: '1px 7px', fontSize: 11
                    }}>
                      {tab.count}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* table */}
        {loading ? (
          <div style={{ padding: 48 }}><UniformLoader message="Loading reviews…" /></div>
        ) : filteredReviews.length === 0 ? (
          <div style={{ padding: '60px 20px', textAlign: 'center' }}>
            <div style={{ fontSize: 40, marginBottom: 12 }}>📭</div>
            <p style={{ color: '#6B7280', margin: 0 }}>No reviews found.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: '#F9FAFB', borderBottom: '2px solid #F3F4F6' }}>
                  {['Customer', 'Rating', 'Comment', 'Order ID', 'Visibility', 'Moderation', 'Actions'].map(h => (
                    <th key={h} style={{
                      padding: '11px 16px', textAlign: 'left', fontSize: 11,
                      fontWeight: 700, color: '#9CA3AF', textTransform: 'uppercase', letterSpacing: '.6px'
                    }}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {paginatedReviews.map((review, idx) => (
                  <tr
                    key={review.id}
                    className="review-row"
                    style={{ borderBottom: idx < paginatedReviews.length - 1 ? '1px solid #F3F4F6' : 'none' }}
                    onMouseEnter={e => (e.currentTarget.style.background = '#FAFAFA')}
                    onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                  >
                    {/* customer */}
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <UserAvatar name={review.userName || 'A'} />
                        <div>
                          <div style={{ fontWeight: 600, fontSize: 14 }}>{review.userName || 'Anonymous'}</div>
                          <div style={{ fontSize: 11, color: '#9CA3AF', marginTop: 2 }}>
                            {review.createdAt?.toDate?.()?.toLocaleDateString('en-US', {
                              month: 'short', day: 'numeric', year: 'numeric'
                            })}
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* rating */}
                    <td style={{ padding: '14px 16px', whiteSpace: 'nowrap' }}>
                      <StarRating rating={review.rating} size={15} />
                      <div style={{ fontSize: 11, color: '#9CA3AF', marginTop: 3 }}>{review.rating}/5</div>
                    </td>

                    {/* comment */}
                    <td style={{ padding: '14px 16px', maxWidth: 280 }}>
                      {review.comment
                        ? <div style={{
                            fontSize: 13, color: '#374151', lineHeight: 1.5,
                            display: '-webkit-box', WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical', overflow: 'hidden'
                          }}>"{review.comment}"</div>
                        : <em style={{ fontSize: 13, color: '#9CA3AF' }}>No comment</em>
                      }
                    </td>

                    {/* order id */}
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{
                        fontFamily: 'monospace', fontSize: 12,
                        background: '#F3F4F6', color: '#374151',
                        padding: '3px 8px', borderRadius: 6
                      }}>
                        {review.orderId.substring(0, 8)}…
                      </span>
                    </td>

                    {/* visibility */}
                    <td style={{ padding: '14px 16px' }}>
                      <VisibilityBadge enabled={review.isEnabled} />
                    </td>

                    {/* moderation */}
                    <td style={{ padding: '14px 16px' }}>
                      <ModerationBadge status={review.moderationStatus} />
                    </td>

                    {/* actions */}
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
                        <ActionBtn
                          label="View"
                          variant="blue"
                          onClick={() => { setSelectedReview(review); setDetailsModal(true); }}
                        />
                        {review.moderationStatus !== 'approved' && (
                          <ActionBtn
                            label="Approve"
                            variant="green"
                            onClick={() => handleUpdateModerationStatus(review.id, 'approved')}
                          />
                        )}
                        {review.moderationStatus !== 'rejected' && (
                          <ActionBtn
                            label="Reject"
                            variant="red"
                            onClick={() => handleUpdateModerationStatus(review.id, 'rejected')}
                          />
                        )}
                        <ActionBtn
                          label={review.isEnabled ? 'Hide' : 'Show'}
                          variant="neutral"
                          onClick={() => handleToggleEnable(review.id, review.isEnabled)}
                        />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* pagination */}
        {!loading && totalPages > 1 && (
          <div style={{
            padding: '12px 20px', borderTop: '1px solid #F3F4F6',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8
          }}>
            <span style={{ fontSize: 13, color: '#6B7280' }}>
              {startIndex + 1}–{Math.min(startIndex + itemsPerPage, filteredReviews.length)} of {filteredReviews.length} reviews
            </span>
            <div style={{ display: 'flex', gap: 6 }}>
              <button
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                style={{
                  padding: '6px 16px', borderRadius: 8, fontSize: 13, fontWeight: 600,
                  border: '1.5px solid #E5E7EB', background: '#fff',
                  cursor: currentPage === 1 ? 'not-allowed' : 'pointer',
                  color: currentPage === 1 ? '#D1D5DB' : '#374151'
                }}
              >
                ← Prev
              </button>
              <span style={{
                padding: '6px 16px', borderRadius: 8, background: '#6366F1',
                color: '#fff', fontSize: 13, fontWeight: 600
              }}>
                {currentPage} / {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                disabled={currentPage >= totalPages}
                style={{
                  padding: '6px 16px', borderRadius: 8, fontSize: 13, fontWeight: 600,
                  border: '1.5px solid #E5E7EB', background: '#fff',
                  cursor: currentPage >= totalPages ? 'not-allowed' : 'pointer',
                  color: currentPage >= totalPages ? '#D1D5DB' : '#374151'
                }}
              >
                Next →
              </button>
            </div>
          </div>
        )}
      </div>

      {/* ── details modal ── */}
      <Modal isOpen={detailsModal} toggle={() => setDetailsModal(false)} size="lg">
        <ModalHeader
          toggle={() => setDetailsModal(false)}
          style={{ borderBottom: '1px solid #F3F4F6', padding: '18px 24px' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {selectedReview && <UserAvatar name={selectedReview.userName || 'A'} />}
            <div>
              <div style={{ fontWeight: 700, fontSize: 16 }}>{selectedReview?.userName || 'Anonymous'}</div>
              <div style={{ fontSize: 12, color: '#9CA3AF', marginTop: 2 }}>
                {selectedReview?.createdAt?.toDate?.()?.toLocaleString()}
              </div>
            </div>
          </div>
        </ModalHeader>

        <ModalBody style={{ padding: 24 }}>
          {selectedReview && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

              {/* rating + comment */}
              <div style={{ background: '#F9FAFB', borderRadius: 12, padding: 20 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
                  <StarRating rating={selectedReview.rating} size={22} />
                  <span style={{ fontWeight: 700, fontSize: 20 }}>{selectedReview.rating}.0</span>
                  <span style={{ color: '#9CA3AF', fontSize: 13 }}>/ 5</span>
                </div>
                <p style={{
                  margin: 0, fontSize: 15, lineHeight: 1.7,
                  fontStyle: selectedReview.comment ? 'normal' : 'italic',
                  color: selectedReview.comment ? '#374151' : '#9CA3AF'
                }}>
                  {selectedReview.comment || 'No comment provided.'}
                </p>
              </div>

              {/* meta */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                {[
                  { label: 'User ID',  value: selectedReview.userId },
                  { label: 'Order ID', value: selectedReview.orderId },
                ].map(item => (
                  <div key={item.label} style={{ background: '#F9FAFB', borderRadius: 10, padding: '12px 16px' }}>
                    <div style={{ fontSize: 11, color: '#9CA3AF', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 6 }}>
                      {item.label}
                    </div>
                    <code style={{ fontSize: 12, color: '#374151', wordBreak: 'break-all' }}>{item.value}</code>
                  </div>
                ))}
              </div>

              {/* moderation panel */}
              <div style={{ background: '#F9FAFB', borderRadius: 12, padding: 20 }}>
                <div style={{ fontSize: 12, fontWeight: 700, color: '#6B7280', textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 14 }}>
                  Moderation Status
                </div>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
                  {(['approved', 'pending', 'rejected'] as const).map(s => {
                    const active = selectedReview.moderationStatus === s;
                    const cfg = {
                      approved: { color: '#16A34A', bg: '#F0FDF4', activeBg: '#16A34A', border: '#BBF7D0', label: '✓ Approve' },
                      pending:  { color: '#B45309', bg: '#FEFCE8', activeBg: '#B45309', border: '#FDE68A', label: '⧖ Pending' },
                      rejected: { color: '#DC2626', bg: '#FEF2F2', activeBg: '#DC2626', border: '#FECACA', label: '✕ Reject'  },
                    }[s];
                    return (
                      <button
                        key={s}
                        onClick={() => handleUpdateModerationStatus(selectedReview.id, s)}
                        style={{
                          padding: '8px 20px', borderRadius: 8, fontSize: 13, fontWeight: 600,
                          border: `1.5px solid ${active ? cfg.activeBg : cfg.border}`,
                          background: active ? cfg.activeBg : cfg.bg,
                          color: active ? '#fff' : cfg.color,
                          cursor: 'pointer', transition: 'all .15s'
                        }}
                      >
                        {cfg.label}
                      </button>
                    );
                  })}
                </div>

                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ fontSize: 12, fontWeight: 700, color: '#6B7280', textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 6 }}>
                      Visibility
                    </div>
                    <VisibilityBadge enabled={selectedReview.isEnabled} />
                  </div>
                  <button
                    onClick={() => handleToggleEnable(selectedReview.id, selectedReview.isEnabled)}
                    style={{
                      padding: '8px 20px', borderRadius: 8, fontSize: 13, fontWeight: 600,
                      border: `1.5px solid ${selectedReview.isEnabled ? '#FECACA' : '#BBF7D0'}`,
                      background: selectedReview.isEnabled ? '#FEF2F2' : '#F0FDF4',
                      color: selectedReview.isEnabled ? '#DC2626' : '#16A34A',
                      cursor: 'pointer'
                    }}
                  >
                    {selectedReview.isEnabled ? 'Hide from app' : 'Show in app'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </ModalBody>

        <ModalFooter style={{ borderTop: '1px solid #F3F4F6', padding: '14px 24px' }}>
          <Button color="secondary" outline onClick={() => setDetailsModal(false)}>Close</Button>
        </ModalFooter>
      </Modal>
    </div>
  );
};

export default ReviewsPage;
