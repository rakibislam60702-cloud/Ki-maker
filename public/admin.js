/**
 * Rakib Official Admin Panel - Key Generator Script
 * Cloudflare Worker Endpoint Integration
 */

window.selectedDays = 3;
window.selectedDeviceLimit = 1;

function selectDays(days) {
  window.selectedDays = days;
  document.querySelectorAll('.day-btn').forEach(btn => {
    btn.classList.toggle('active', parseInt(btn.dataset.days) === days);
  });
  const customInput = document.getElementById('customDaysInput');
  if (customInput) customInput.value = '';
}

function selectDeviceLimit(limit) {
  window.selectedDeviceLimit = limit;
  document.querySelectorAll('.device-btn').forEach(btn => {
    btn.classList.toggle('active', parseInt(btn.dataset.limit) === limit);
  });
}

async function generateAndDeployKey() {
  const customKeyInput = document.getElementById("customKeyInput");
  const validityDays = window.selectedDays || 3;
  const deviceLimit = window.selectedDeviceLimit || 1;

  const keyName = (customKeyInput && customKeyInput.value.trim()) 
    ? customKeyInput.value.trim() 
    : "AIM-" + Math.random().toString(36).substring(2, 10).toUpperCase();

  const endpoint = "https://misty-bush-77a9.rakibul74348.workers.dev/api/admin/create-key";

  const btn = document.getElementById("deployBtn");
  if (btn) {
    btn.disabled = true;
    btn.innerText = "Connecting to Worker...";
  }

  try {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        key: keyName,
        days: validityDays,
        deviceLimit: deviceLimit,
        user: "Admin Created"
      })
    });

    const result = await response.json();

    if (result.success) {
      alert("Key Created: " + result.key);
      if (customKeyInput) customKeyInput.value = "";
    } else {
      alert("Error: " + (result.error || "Failed to create key"));
    }
  } catch (err) {
    alert("Connection Error: " + err.message);
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerText = "Generate & Deploy Key";
    }
  }
}

window.generateAndDeployKey = generateAndDeployKey;
window.selectDays = selectDays;
window.selectDeviceLimit = selectDeviceLimit;
