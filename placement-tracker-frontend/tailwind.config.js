/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {},
  },
  corePlugins: {
    // Preflight resets margins/borders/list-styles/etc. project-wide, which would clobber
    // the existing hand-written design system in index.css (built without Preflight in
    // mind, across every page in the app). Disabled so Tailwind only ever adds utility
    // classes to the new components that use them, never changes anything else.
    preflight: false,
  },
  plugins: [],
};
