document.body.className = 'js-enabled';

$(document).ready(function () {
    // Use GOV.UK shim-links-with-button-role.js to trigger a link styled to look like a button,
    // with role="button" when the space key is pressed.
    GOVUK.shimLinksWithButtonRole.init()

    // Initialise details component
    GOVUK.details.init();

    let errors = document.getElementById("errors")
    if(errors) errors.focus();
})