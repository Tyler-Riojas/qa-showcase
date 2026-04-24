document.getElementById('action-btn').addEventListener('click', function() {
  document.getElementById('result').textContent = 'Check complete!';
  document.getElementById('result').setAttribute('data-status', 'complete');
});
