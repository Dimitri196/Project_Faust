/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          dark: '#020617',
          panel: '#0f172a',
          accent: '#3b82f6',
          border: '#1e293b',
        }
      }
    },
  },
  plugins: [],
}