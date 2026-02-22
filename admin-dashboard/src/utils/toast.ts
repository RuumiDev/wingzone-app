import Swal from 'sweetalert2';

/**
 * Display a toast notification
 * @param icon - 'success' or 'error'
 * @param title - The message to display
 */
export const showToast = (
  icon: 'success' | 'error',
  title: string
): void => {
  const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
  });

  Toast.fire({
    icon,
    title
  });
};
