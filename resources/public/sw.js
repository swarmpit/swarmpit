importScripts('https://storage.googleapis.com/workbox-cdn/releases/3.6.1/workbox-sw.js');

workbox.routing.registerRoute(
    new RegExp('.*\.js'),
    workbox.strategies.networkFirst()
);

workbox.routing.registerRoute(
    /.*\.css/,
    workbox.strategies.staleWhileRevalidate({
        cacheName: 'css-cache'
    })
);

workbox.routing.registerRoute(
    /.*\.(?:png|jpg|jpeg|svg|gif)/,
    workbox.strategies.cacheFirst({
        cacheName: 'image-cache',
        plugins: [
            new workbox.expiration.Plugin({
                maxEntries: 20,
                maxAgeSeconds: 7 * 24 * 60 * 60
            })
        ]
    })
);
