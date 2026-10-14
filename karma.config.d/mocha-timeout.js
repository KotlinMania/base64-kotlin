const client = config.client || {};

config.set({
  browserNoActivityTimeout: 120000,
  browserDisconnectTimeout: 120000,
  browserDisconnectTolerance: 1,
  client: {
    ...client,
    mocha: {
      ...(client.mocha || {}),
      timeout: 120000
    }
  },
  customLaunchers: {
    ChromeHeadlessMemory: {
      base: 'ChromeHeadless',
      flags: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--js-flags=--max-old-space-size=4096'
      ]
    }
  },
  browsers: ['ChromeHeadlessMemory']
});
