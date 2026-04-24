(function() {
  if (document.getElementById('qa-demo-banner')) return;
  var banner = document.createElement('div');
  banner.id = 'qa-demo-banner';
  banner.setAttribute('data-testid', 'qa-demo-banner');
  banner.textContent = 'QA Demo Extension Active';
  banner.style.cssText = 'position:fixed;top:0;left:0;width:100%;background:#1a73e8;color:white;text-align:center;padding:8px;z-index:999999;font-family:Arial;font-size:14px;';
  document.body.appendChild(banner);
})();
