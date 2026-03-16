function handleProtectedPageLoad(auth, options = {}) {
  if (!auth || typeof auth.requireLogin !== 'function') {
    return false
  }
  if (!auth.requireLogin()) {
    return false
  }
  if (typeof options.beforeInit === 'function' && options.beforeInit() === false) {
    return false
  }
  if (typeof options.afterInit === 'function') {
    options.afterInit()
  }
  return true
}

function handleProtectedPageShow(auth, options = {}) {
  if (!auth || typeof auth.requireLogin !== 'function') {
    return false
  }
  if (!auth.requireLogin()) {
    return false
  }
  if (typeof options.beforeShow === 'function') {
    options.beforeShow()
  }
  if (typeof options.afterShow === 'function') {
    options.afterShow()
  }
  return true
}

module.exports = {
  handleProtectedPageLoad,
  handleProtectedPageShow
}
