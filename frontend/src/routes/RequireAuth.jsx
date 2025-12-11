import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function RequireAuth({ children }) {
  const location = useLocation();
  const { isAuthenticated, initialised } = useAuth();

  if (!initialised) {
    return (
      <div className="min-h-[60vh] grid place-items-center text-sm text-stone-500">
        Đang kiểm tra phiên đăng nhập...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  return children;
}
