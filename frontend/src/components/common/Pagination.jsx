import { formatNumber } from '../../utils/format';

/** Controls for the backend PageResponse { page (0-based), size, totalElements, totalPages, first, last }. */
export function Pagination({ pageData, onPageChange }) {
  if (!pageData || !pageData.totalPages || pageData.totalPages <= 1) {
    return pageData?.totalElements ? (
      <div className="pagination subtle">{formatNumber(pageData.totalElements)} total</div>
    ) : null;
  }
  const { page, size, totalElements, totalPages, first, last } = pageData;
  const start = page * size + 1;
  const end = Math.min(totalElements, (page + 1) * size);
  return (
    <nav className="pagination" aria-label="Pagination">
      <span className="subtle">
        {formatNumber(start)}–{formatNumber(end)} of {formatNumber(totalElements)}
      </span>
      <div className="row">
        <button type="button" className="btn btn-sm" disabled={first} onClick={() => onPageChange(page - 1)}>
          Previous
        </button>
        <span className="subtle">
          Page {page + 1} of {totalPages}
        </span>
        <button type="button" className="btn btn-sm" disabled={last} onClick={() => onPageChange(page + 1)}>
          Next
        </button>
      </div>
    </nav>
  );
}
