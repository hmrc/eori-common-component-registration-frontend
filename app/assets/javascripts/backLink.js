const docReferrer = document.referrer;

if (
  window.history &&
  window.history.replaceState &&
  typeof window.history.replaceState === 'function'
) {
    window.history.replaceState(null, null, window.location.href);

    document.getElementById('back-link').addEventListener('click', function (e) {
    e.preventDefault();
    if (
        window.history &&
        window.history.back &&
        typeof window.history.back === 'function' &&
        docReferrer !== '' &&
        docReferrer.indexOf(window.location.host) !== -1) {
        window.history.back();
    }
    });
}

