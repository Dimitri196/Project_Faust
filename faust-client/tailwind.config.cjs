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
      },
      // --- PŘIDÁNO PRO EFEKTY TAJNÉ SLUŽBY ---
      keyframes: {
        'scan-line': {
          '0%': { top: '0%' },
          '100%': { top: '100%' }
        },
        'pulse-glow': {
          '0%, 100%': { opacity: '1', filter: 'brightness(1)' },
          '50%': { opacity: '0.7', filter: 'brightness(1.5) drop-shadow(0 0 2px #3b82f6)' }
        }
      },
      animation: {
        'scan-line': 'scan-line 3s linear infinite',
        'pulse-glow': 'pulse-glow 4s ease-in-out infinite'
      }
    },
  },
  plugins: [],
}