const DOCUMENTATION_BASE_URL = (
  import.meta.env.VITE_DOCUMENTATION_BASE_URL || '/blog'
).replace(/\/+$/, '')

export const DOCUMENTATION_LINKS = Object.freeze({
  documentationHome: {
    en: '/documentation/',
    pl: '/pl/dokumentacja/',
  },
  systemOverview: {
    en: '/documentation/dashboard-system-overview',
    pl: '/pl/dokumentacja/dashboard-system-overview',
    sections: {
      requests: 'requests',
      refresh: 'refresh',
    },
  },
  previousDayClose: {
    en: '/documentation/previous-day-close-summary',
    pl: '/pl/dokumentacja/podsumowanie-poprzedniego-dnia',
    sections: {
      readPath: 'read-path',
      production: 'production',
      failures: 'failures',
    },
  },
  recentTransactions: {
    en: '/documentation/recent-transactions',
    pl: '/pl/dokumentacja/ostatnie-transakcje',
    sections: {
      requestPath: 'request-path',
      limit: 'limit',
      refresh: 'refresh',
    },
  },
  quickActions: {
    en: '/documentation/quick-actions',
    pl: '/pl/dokumentacja/szybkie-akcje',
    sections: {
      routes: 'routes',
      submissionBoundary: 'submission-boundary',
    },
  },
  mbgate: {
    en: '/documentation/mbgate',
    pl: '/pl/dokumentacja/mbgate',
  },
  mbresult: {
    en: '/documentation/mbresult',
    pl: '/pl/dokumentacja/mbresult',
  },
  mvsInstallation: {
    en: '/documentation/mvs-installation',
    pl: '/pl/dokumentacja/instalacja-mvs',
  },
  addCustomer: {
    en: '/documentation/add-customer',
    pl: '/pl/dokumentacja/dodaj-klienta',
  },
  customers: {
    en: '/documentation/customers',
    pl: '/pl/dokumentacja/klienci',
    sections: {
      list: 'list',
      localView: 'local-view',
      status: 'status',
    },
  },
  liveConsole: {
    en: '/documentation/live-mainframe-console',
    pl: '/pl/dokumentacja/konsola-mainframe-live',
    sections: {
      sources: 'sources',
      reconnection: 'reconnection',
      activity: 'activity',
      security: 'security',
    },
  },
})

export function documentationUrl(pageKey, options = {}) {
  const page = DOCUMENTATION_LINKS[pageKey]

  if (!page) {
    throw new Error(`Unknown documentation page: ${pageKey}`)
  }

  const locale = options.locale || 'en'
  const path = page[locale]

  if (!path) {
    throw new Error(`Documentation page ${pageKey} has no ${locale} route`)
  }

  const section = options.section
  const anchor = section ? page.sections?.[section] : null

  if (section && !anchor) {
    throw new Error(`Documentation page ${pageKey} has no ${section} section`)
  }

  return `${DOCUMENTATION_BASE_URL}${path}${anchor ? `#${anchor}` : ''}`
}
