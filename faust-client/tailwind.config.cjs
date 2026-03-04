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
      backgroundImage: {
        'grid-pattern': "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='40' height='40' viewBox='0 0 40 40'%3E%3Cpath d='M0 0h1v40H0V0zm1 0h39v1H1V0z' fill='%231e293b' fill-opacity='0.3'/%3E%3C/svg%3E\")",
      },
      keyframes: {
        'scan-line': {
          '0%': { top: '0%' },
          '100%': { top: '100%' }
        },
        'pulse-glow': {
          '0%, 100%': { opacity: '1', filter: 'brightness(1)' },
          '50%': { opacity: '0.7', filter: 'brightness(1.5) drop-shadow(0 0 5px #10b981)' }
        },
        // PŘIDÁNO: Animace pro nekonečný text
        'marquee': {
          '0%': { transform: 'translateX(0%)' },
          '100%': { transform: 'translateX(-50%)' }
        }
      },
      animation: {
        'scan-line': 'scan-line 4s linear infinite',
        'pulse-glow': 'pulse-glow 2s ease-in-out infinite',
        // PŘIDÁNO: 15s cyklus, lineární pohyb
        'marquee-slow': 'marquee 15s linear infinite',
      }
    },
  },
  plugins: [],
}