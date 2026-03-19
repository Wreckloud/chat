function runPullDownRefresh(page, task) {
  if (!page || typeof task !== 'function') {
    wx.stopPullDownRefresh()
    return
  }

  if (page.__pullDownRefreshing) {
    wx.stopPullDownRefresh()
    return
  }

  page.__pullDownRefreshing = true
  Promise.resolve()
    .then(() => task())
    .finally(() => {
      page.__pullDownRefreshing = false
      wx.stopPullDownRefresh()
    })
}

module.exports = {
  runPullDownRefresh
}
