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
  ['meta', { property: 'og:site_name', content: 'MoniBank Engineering' }],
  ['script', {
    type: 'module',
    src: 'https://static.cloudflareinsights.com/beacon.min.js',
    'data-cf-beacon': '{"token":"0e80c27d25fa41c4926dedb8797a6673"}'
  }]

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
          { text: 'Documentation', link: '/documentation/' },
          {
  text: 'Articles',
  items: [
    {
      text: 'GET CUSTOMER integration',
      link: '/articles/get-customer'
    },
    {
      text: 'JCL and COBOL automation',
      link: '/articles/jcl-cobol-automation'
    },
    {
      text: 'Three terminals, two workers',
      link: '/articles/terminal-workers'
    }
  ]
},
          { text: 'Development', link: '/#development' }
        ],
        sidebar: {
          '/documentation/': [
            {
              text: 'MoniBank documentation',
              items: [
                { text: 'Documentation map', link: '/documentation/' },
                { text: 'Dashboard system overview', link: '/documentation/dashboard-system-overview' },
                { text: 'Previous-day close summary', link: '/documentation/previous-day-close-summary' },
                { text: 'Recent transactions', link: '/documentation/recent-transactions' },
                { text: 'Dashboard quick actions', link: '/documentation/quick-actions' },
                { text: 'Customers', link: '/documentation/customers' },
                { text: 'Live mainframe console', link: '/documentation/live-mainframe-console' },
                { text: 'MBGATE gateway', link: '/documentation/mbgate' },
                { text: 'MBRESULT result channel', link: '/documentation/mbresult' },
                { text: 'Installing on MVS', link: '/documentation/mvs-installation' },
                { text: 'Add customer flow', link: '/documentation/add-customer' }
              ]
            }
          ]
        },
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
          { text: 'Dokumentacja', link: '/pl/dokumentacja/' },
          {
  text: 'Artykuły',
  items: [
    {
      text: 'Integracja GET CUSTOMER',
      link: '/pl/articles/get-customer'
    },
    {
      text: 'Automatyzacja JCL i COBOL',
      link: '/pl/articles/jcl-cobol-automation'
    },
    {
      text: 'Trzy terminale, dwa workery',
      link: '/pl/articles/terminal-workers'
    }
  ]
},
          { text: 'Rozwój', link: '/pl/#rozwoj' }
        ],
        sidebar: {
          '/pl/dokumentacja/': [
            {
              text: 'Dokumentacja MoniBanku',
              items: [
                { text: 'Mapa dokumentacji', link: '/pl/dokumentacja/' },
                { text: 'Górny panel dashboardu', link: '/pl/dokumentacja/dashboard-system-overview' },
                { text: 'Podsumowanie poprzedniego dnia', link: '/pl/dokumentacja/podsumowanie-poprzedniego-dnia' },
                { text: 'Ostatnie transakcje', link: '/pl/dokumentacja/ostatnie-transakcje' },
                { text: 'Szybkie akcje dashboardu', link: '/pl/dokumentacja/szybkie-akcje' },
                { text: 'Klienci', link: '/pl/dokumentacja/klienci' },
                { text: 'Konsola mainframe na żywo', link: '/pl/dokumentacja/konsola-mainframe-live' },
                { text: 'Bramka MBGATE', link: '/pl/dokumentacja/mbgate' },
                { text: 'Kanał wynikowy MBRESULT', link: '/pl/dokumentacja/mbresult' },
                { text: 'Instalacja na MVS', link: '/pl/dokumentacja/instalacja-mvs' },
                { text: 'Proces dodawania klienta', link: '/pl/dokumentacja/dodaj-klienta' }
              ]
            }
          ]
        },
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
