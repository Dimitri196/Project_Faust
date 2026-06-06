/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      // Definujeme měřítko pro Macro-UI (vše je o 20-30% větší)
      fontSize: {
        'macro-xs': '0.75rem',   // standard 12px
        'macro-sm': '1rem',      // standard 16px
        'macro-base': '1.25rem', // standard 20px
        'macro-xl': '2rem',
        'macro-2xl': '3.5rem',
      },
      colors: {
        brand: {
          dark: '#020617',
          panel: '#0a0f1d', // Trochu tmavší pro hloubku
          accent: '#3b82f6',
          border: '#1e293b',
          glow: 'rgba(59, 130, 246, 0.5)',
        }
      },
      backgroundImage: {
        // Výraznější grid pro 50% zoom
        'grain-pattern': "url(\"data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E\")",
      },
      keyframes: {
        'scan-line': {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(1000%)' }
        },
        'pulse-glow': {
          '0%, 100%': { opacity: '1', filter: 'brightness(1) blur(0px)' },
          '50%': { opacity: '0.8', filter: 'brightness(1.8) blur(2px)' }
        },
        'marquee': {
          '0%': { transform: 'translateX(0%)' },
          '100%': { transform: 'translateX(-50%)' }
        },
        // PRIDÁNO: Čistá hardwarová animace pro větve stromu
        'fade-in-slide': {
          '0%': { opacity: '0', transform: 'translateX(-4px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' }
        }
      },
      animation: {
        'scan-line-fast': 'scan-line 3s linear infinite',
        'pulse-glow-heavy': 'pulse-glow 3s ease-in-out infinite',
        'marquee-slow': 'marquee 25s linear infinite',
        // PRIDÁNO: Extrémně rychlá animace (150ms), která neblokuje CPU
        'node-appear': 'fade-in-slide 0.15s cubic-bezier(0.16, 1, 0.3, 1) forwards',
      }
    },
  },
  plugins: [],
}