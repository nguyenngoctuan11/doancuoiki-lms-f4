/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./public/index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        primary: {
          50: "#F3ECE6",
          100: "#E8D9CD",
          200: "#D2B49C",
          300: "#BB906C",
          400: "#A46B3B",
          500: "#8B5E34",
          600: "#734C29",
          700: "#5B3C20",
          800: "#432D18",
          900: "#2B1E10",
        },
        accent: {
          500: "#C9A368",
          600: "#B68E4F",
        },
      },
    },
  },
  plugins: [],
};
