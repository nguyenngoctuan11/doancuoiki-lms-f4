import { createBrowserRouter, RouterProvider } from "react-router-dom";
import App from "../App";
import Login from "../pages/Login";
import Register from "../pages/Register";
import ForgotPassword from "../pages/ForgotPassword";
import Home from "../pages/Home";
import Courses from "../pages/Courses";
import CourseDetail from "../pages/CourseDetail";
import ExamPlayer from "../pages/exams/ExamPlayer";
import Blog from "../pages/Blog";
import BlogDetail from "../pages/BlogDetail";
import BlogStudio from "../pages/blog/BlogStudio";
import Survey from "../pages/Survey";
import PathDetail from "../pages/PathDetail";
import StudentDashboard from "../pages/student/StudentDashboard";
import MyCourses from "../pages/MyCourses";
import Mentors from "../pages/Mentors";
import FAQ from "../pages/FAQ";
import About from "../pages/About";
import Contact from "../pages/Contact";
import PlacementTest from "../pages/tests/PlacementTest";
import NotFound from "../pages/NotFound";
import LearnLayout from "../pages/learn/LearnLayout";
import CoursePlayer from "../pages/learn/CoursePlayer";
import Checkout from "../pages/checkout/Checkout";
import CheckoutSuccess from "../pages/checkout/CheckoutSuccess";
import CheckoutFailed from "../pages/checkout/CheckoutFailed";
import AccountSettings from "../pages/account/AccountSettings";
import SupportInbox from "../pages/manager/SupportInbox";
import RequireAuth from "./RequireAuth";
import { AuthProvider } from "../context/AuthContext";
import { SupportChatProvider } from "../context/SupportChatContext";

const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      { index: true, element: <Home /> },
      { path: "home", element: <Home /> },
      { path: "courses", element: <Courses /> },
      { path: "survey", element: <Survey /> },
      { path: "placement-test", element: <PlacementTest /> },

      { path: "paths/:id", element: <PathDetail /> },
      { path: "student", element: <StudentDashboard /> },
      { path: "my-courses", element: <MyCourses /> },
      { path: "courses/:slug", element: <CourseDetail /> },
      {
        path: "courses/:courseId/exams/:examId",
        element: (
          <RequireAuth>
            <ExamPlayer />
          </RequireAuth>
        ),
      },
      { path: "blog", element: <Blog /> },
      { path: "blog/:slug", element: <BlogDetail /> },
      {
        path: "blog/studio",
        element: (
          <RequireAuth>
            <BlogStudio />
          </RequireAuth>
        ),
      },
      { path: "mentors", element: <Mentors /> },
      { path: "faq", element: <FAQ /> },
      { path: "about", element: <About /> },
      { path: "contact", element: <Contact /> },
      { path: "checkout", element: <Checkout /> },
      { path: "checkout/success", element: <CheckoutSuccess /> },
      { path: "checkout/failed", element: <CheckoutFailed /> },
      {
        path: "account/settings",
        element: (
          <RequireAuth>
            <AccountSettings />
          </RequireAuth>
        ),
      },
      {
        path: "learn/:courseId",
        element: <LearnLayout />,
        children: [
          { index: true, element: <CoursePlayer /> },
          { path: "lesson/:lessonId", element: <CoursePlayer /> },
        ],
      },
      {
        path: "manager/support",
        element: (
          <RequireAuth>
            <SupportInbox />
          </RequireAuth>
        ),
      },
      { path: "login", element: <Login /> },
      { path: "register", element: <Register /> },
      { path: "forgot-password", element: <ForgotPassword /> },
      { path: "*", element: <NotFound /> },
    ],
  },
]);

export default function AppRouter() {
  return (
    <AuthProvider>
      <SupportChatProvider>
        <RouterProvider router={router} />
      </SupportChatProvider>
    </AuthProvider>
  );
}
