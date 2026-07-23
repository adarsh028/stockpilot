/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        // Professional enterprise blue — confident, restrained, premium.
        brand: {
          50: "#eff4ff",
          100: "#dbe6fe",
          200: "#bfd3fe",
          300: "#93b4fd",
          400: "#608dfa",
          500: "#3b6af5",
          600: "#2450e0",
          700: "#1e42c4",
          800: "#1e39a0",
          900: "#1e347f",
        },
      },
      fontFamily: {
        sans: [
          "Inter",
          "ui-sans-serif",
          "system-ui",
          "-apple-system",
          "Segoe UI",
          "Roboto",
          "Helvetica Neue",
          "Arial",
          "sans-serif",
        ],
        mono: ["ui-monospace", "SFMono-Regular", "Menlo", "Consolas", "monospace"],
      },
      boxShadow: {
        xs: "0 1px 2px 0 rgb(16 24 40 / 0.05)",
        sm: "0 1px 3px 0 rgb(16 24 40 / 0.08), 0 1px 2px -1px rgb(16 24 40 / 0.06)",
        card: "0 1px 2px 0 rgb(16 24 40 / 0.04), 0 1px 3px 0 rgb(16 24 40 / 0.06)",
        "card-hover":
          "0 4px 12px -2px rgb(16 24 40 / 0.10), 0 2px 6px -2px rgb(16 24 40 / 0.05)",
        dropdown:
          "0 8px 24px -6px rgb(16 24 40 / 0.14), 0 4px 8px -4px rgb(16 24 40 / 0.06)",
        modal: "0 24px 48px -12px rgb(16 24 40 / 0.28)",
      },
      borderRadius: {
        xl: "0.75rem",
        "2xl": "1rem",
      },
      keyframes: {
        "fade-in": {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        "scale-in": {
          "0%": { opacity: "0", transform: "translateY(6px) scale(0.985)" },
          "100%": { opacity: "1", transform: "translateY(0) scale(1)" },
        },
        "slide-up": {
          "0%": { opacity: "0", transform: "translateY(6px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
      },
      animation: {
        "fade-in": "fade-in 0.15s ease-out",
        "scale-in": "scale-in 0.16s cubic-bezier(0.16, 1, 0.3, 1)",
        "slide-up": "slide-up 0.2s ease-out",
      },
    },
  },
  plugins: [],
};
