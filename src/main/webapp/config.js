const descriptorUrl = "/manage/descriptorByName/io.jenkins.plugins.railflow.jenkins.admin.GlobalConfig";

function uploadLicense() {
    try {
        const error = document.getElementById("railflow-license-upload-error");
        const spinner = document.getElementById("railflow-license-upload-spinner");
        clearStatus(error, spinner);

        const files = document.getElementById("railflow-license-chooser");
        const data = new FormData();
        data.append("licenseFile", files.files[0])

        const request = new Request(descriptorUrl + "/uploadLicenseFile");
        const options = {
            headers: {
                "Accept": "application/json",
                "Jenkins-Crumb": crumb.value
            },
            method: "POST",
            body: data
        };

        fetch(request, options)
        .then(function(response) {
            spinner.style.display = "none";
            if (response.status >= 200 && response.status < 400) {
                response.json().then(function(json) {
                    setLicenseInfo(json.data);
                    showMessage(error, "success", json.data.message);
                });
            } else {
                response.text().then(function(text) {
                    showMessage(error, "error", text);
                });
            }
        })
        .catch(function(response) {
            resetStatus(error, spinner);
            console.error(response);
        });
    } catch (e) {
        console.error(e);
    }
}

function activateLicense() {
    try {
        const error = document.getElementById("railflow-license-activate-error");
        const spinner = document.getElementById("railflow-license-activate-spinner");
        clearStatus(error, spinner);

        const parameters = new URLSearchParams({
            licenseKey: document.getElementById('railflow-license-key').value
        });

        const request = new Request(descriptorUrl + "/activateLicenseKey?" + parameters.toString());
        const options = {
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json",
                "Jenkins-Crumb": crumb.value
            },
            method: "POST"
        };

        fetch(request, options)
        .then(function(response) {
            spinner.style.display = "none";
            if (response.status >= 200 && response.status < 400) {
                response.json().then(function(json) {
                    setLicenseInfo(json.data);
                    showMessage(error, "success", json.data.message);
                });
            } else {
                response.text().then(function(text) {
                    showMessage(error, "error", text);
                });
            }
        })
        .catch(function(response) {
            resetStatus(error, spinner);
            console.error(response);
        });
    } catch (e) {
        console.error(e);
    }
}

function setLicenseInfo(data) {
    document.getElementById("expirationDate").value = data.expirationDate ? data.expirationDate : "N/A";
    document.getElementById("licenseContent").value = data.licenseContent;
    document.getElementById("onlineActivation").checked = (data.onlineActivation === 'true');
    document.getElementById("trial").checked = (data.trial === 'true');
}

function clearStatus(error, spinner) {
    error.classList.remove("error", "success");
    error.style.display = "none";
    spinner.style.display = "block";
}

function resetStatus(error, spinner) {
    error.style.display = "none";
    spinner.style.display = "none";
}

function showMessage(div, style, msg) {
    div.style.display = "block";
    div.classList.add(style);
    div.innerHTML = msg;
}

document.getElementById("railflow-activate-license-button").onclick = activateLicense;
document.getElementById("railflow-upload-license-button").onclick = uploadLicense;
