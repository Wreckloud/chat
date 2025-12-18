/**
 * 登录页面
 * @author Wreckloud
 * @date 2024-12-18
 */

const accountApi = require('../../api/account.js');
const auth = require('../../utils/auth.js');
const logger = require('../../utils/logger.js');

Page({
  data: {
    mobile: '',
    smsCode: '',
    smsCodeKey: '',
    countdown: 0,
    loginLoading: false
  },

  /**
   * 页面加载
   */
  onLoad(options) {
    logger.lifecycle('LoginPage', 'onLoad', options);
    
    // 如果已登录，直接跳转到首页
    if (auth.isLogin()) {
      logger.info('LoginPage', '已登录，跳转到首页');
      wx.reLaunch({
        url: '/pages/tabs/tabs'
      });
    }
  },

  /**
   * 手机号输入
   */
  onMobileChange(e) {
    this.setData({
      mobile: e.detail
    });
  },

  /**
   * 验证码输入
   */
  onSmsCodeChange(e) {
    this.setData({
      smsCode: e.detail
    });
  },

  /**
   * 发送验证码
   */
  sendSmsCode() {
    const { mobile } = this.data;

    logger.action('LoginPage', 'sendSmsCode', { mobile });

    // 验证手机号
    if (!mobile) {
      logger.warn('LoginPage', '手机号为空');
      wx.showToast({
        title: '请输入手机号',
        icon: 'none'
      });
      return;
    }

    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      logger.warn('LoginPage', '手机号格式不正确', { mobile });
      wx.showToast({
        title: '手机号格式不正确',
        icon: 'none'
      });
      return;
    }

    // 发送验证码
    logger.info('LoginPage', '开始发送验证码', { mobile });
    accountApi.sendSmsCode(mobile)
      .then(res => {
        logger.info('LoginPage', '验证码发送成功', res);
        wx.showToast({
          title: '验证码已发送',
          icon: 'success'
        });

        // 保存验证码key
        this.setData({
          smsCodeKey: res.smsCodeKey
        });

        // 开始倒计时
        this.startCountdown();
      })
      .catch(err => {
        logger.error('LoginPage', '发送验证码失败', err);
      });
  },

  /**
   * 开始倒计时
   */
  startCountdown() {
    let countdown = 60;
    this.setData({ countdown });

    const timer = setInterval(() => {
      countdown--;
      this.setData({ countdown });

      if (countdown === 0) {
        clearInterval(timer);
      }
    }, 1000);
  },

  /**
   * 登录
   */
  login() {
    const { mobile, smsCode, smsCodeKey } = this.data;

    logger.action('LoginPage', 'login', { mobile });

    // 验证
    if (!mobile) {
      logger.warn('LoginPage', '手机号为空');
      wx.showToast({
        title: '请输入手机号',
        icon: 'none'
      });
      return;
    }

    if (!smsCode) {
      logger.warn('LoginPage', '验证码为空');
      wx.showToast({
        title: '请输入验证码',
        icon: 'none'
      });
      return;
    }

    if (!smsCodeKey) {
      logger.warn('LoginPage', '未获取验证码Key');
      wx.showToast({
        title: '请先获取验证码',
        icon: 'none'
      });
      return;
    }

    // 显示加载
    this.setData({ loginLoading: true });

    // 调用登录接口
    logger.info('LoginPage', '开始登录', { mobile, smsCodeKey });
    accountApi.loginByMobile({
      mobile,
      smsCodeKey,
      smsCode
    })
      .then(res => {
        logger.info('LoginPage', '登录成功', { userId: res.userId, wfNo: res.wfNo });
        
        // 保存Token和用户信息
        auth.setToken(res.token);
        auth.setUser({
          userId: res.userId,
          wfNo: res.wfNo,
          username: res.username,
          avatar: res.avatar
        });

        wx.showToast({
          title: '登录成功',
          icon: 'success',
          duration: 1500
        });

        // 跳转到首页
        setTimeout(() => {
          logger.info('LoginPage', '跳转到首页');
          wx.reLaunch({
            url: '/pages/tabs/tabs'
          });
        }, 1500);
      })
      .catch(err => {
        logger.error('LoginPage', '登录失败', err);
        this.setData({ loginLoading: false });
      });
  }
});

