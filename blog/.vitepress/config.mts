import { defineConfig } from 'vitepress'
import { MermaidMarkdown } from 'vitepress-plugin-mermaid'

const githubUrl = 'https://github.com/StormSister/MoniBank'

export default defineConfig({
  title: 'MoniBank Engineering',
  description: 'Modern APIs meeting MVS 3.8J, KICKS, COBOL, VSAM and JES.',
  lang: 'en',
  base: '/blog/',
  cleanUrls: true,
  srcExclude: [
    'video/**',
    'pl/video/**'
  ],
  lastUpdated: true,
  appearance: 'dark',
  head: [
  ['link', {
    rel: 'icon',
    type: 'image/png',
    href: '/blog/favicon.png'
  }],
  ['link', {
    rel: 'apple-touch-icon',
    href: '/blog/apple-touch-icon.png'
  }],
  ['meta', { name: 'theme-color', content: '#07131d' }],
  ['meta', { name: 'theme-color', content: '#07131d' }],
  ['meta', { property: 'og:type', content: 'website' }],
  ['meta', { property: 'og:site_name', content: 'MoniBank Engineering' }]

],
  markdown: {
  lineNumbers: true,
  config(md) {
    MermaidMarkdown(md)
  }
},
locales: {
    root: {
      label: 'English',
      lang: 'en',
      title: 'MoniBank Engineering',
      description: 'Modern APIs meeting MVS 3.8J, KICKS, COBOL, VSAM and JES.',
      themeConfig: {
        nav: [
          { text: 'Home', link: '/' },
          { text: 'Project', link: '/#project' },
          { text: 'Articles', link: '/articles/get-customer' },
          { text: 'Development', link: '/#development' }
        ],
        outline: { level: [2, 3], label: 'On this page' },
        docFooter: { prev: 'Previous page', next: 'Next page' },
        lastUpdated: { text: 'Last updated' },
        returnToTopLabel: 'Return to top',
        sidebarMenuLabel: 'Menu',
        darkModeSwitchLabel: 'Theme',
        langMenuLabel: 'Language'
      }
    },
    pl: {
      label: 'Polski',
      lang: 'pl',
      link: '/pl/',
      title: 'MoniBank Engineering',
      description: 'Nowoczesne API spotyka MVS 3.8J, KICKS, COBOL, VSAM i JES.',
      themeConfig: {
        nav: [
          { text: 'Start', link: '/pl/' },
          { text: 'Projekt', link: '/pl/#projekt' },
          { text: 'Artykuły', link: '/pl/articles/get-customer' },
          { text: 'Rozwój', link: '/pl/#rozwoj' }
        ],
        outline: { level: [2, 3], label: 'Na tej stronie' },
        docFooter: { prev: 'Poprzednia strona', next: 'Następna strona' },
        lastUpdated: { text: 'Ostatnia aktualizacja' },
        returnToTopLabel: 'Wróć na górę',
        sidebarMenuLabel: 'Menu',
        darkModeSwitchLabel: 'Motyw',
        langMenuLabel: 'Język'
      }
    }
  },
  themeConfig: {
    logo: {
      light: '/monibank-mark.png',
      dark: '/monibank-mark.png',
      alt: 'MoniBank'
    },
    search: {
      provider: 'local'
    },
    socialLinks: [
      { icon: 'github', link: githubUrl },
      {
      icon: 'linkedin',
      link: 'https://www.linkedin.com/in/monika-gudalewska/'
      }
    ],
    footer: {
      message: 'Core Banking · Mainframe Engineering',
      copyright: 'Copyright © 2026 MoniBank'
    }
  }
})
