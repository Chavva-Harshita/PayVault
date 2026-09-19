/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // "Vault" token system - a passbook/ledger aesthetic, not the
        // generic warm-cream-plus-terracotta look. Cool paper, deep ink,
        // and a bank-ledger green as the primary accent instead.
        ink: {
          DEFAULT: '#14213D',
          light: '#1F3357',
        },
        paper: {
          DEFAULT: '#EFF1EC',
          raised: '#F8F9F6',
        },
        vault: {
          DEFAULT: '#1F6F54',
          dark: '#17543F',
          light: '#2C8A69',
        },
        amber: {
          DEFAULT: '#C98A3B',
        },
        clay: {
          DEFAULT: '#A6483D',
        },
        fade: {
          DEFAULT: '#5B6560',
        },
        line: {
          DEFAULT: '#D9DCD3',
        },
      },
      fontFamily: {
        display: ['"Fraunces"', 'serif'],
        sans: ['"Manrope"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
    },
  },
  plugins: [],
}
