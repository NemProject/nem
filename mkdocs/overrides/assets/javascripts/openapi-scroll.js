(function() {
	window.onload = function() {
		const anchor = window.location.hash;
		if (anchor) {
			scrollToIframeIdAndHighlight(anchor.substring(1));
		}
	};

	// Scrolls to and highlights an element within an iframe
	async function scrollToIframeIdAndHighlight(elementSelector) {
		try {
			const iframes = await waitFor(() => document.getElementsByClassName("swagger-ui-iframe"));
			const iframe = iframes[0];

			if (iframe && iframe.contentWindow) {
				if (iframe.contentWindow.document.readyState === "complete") {
					const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
					const element = await waitFor(() => iframeDoc.querySelector(`[id='${elementSelector}']`));

					if (element) {
						// Ensure the operation detail is expanded before scrolling
						const control = element.querySelector(".opblock-summary-control");
						if (control) {
							control.click();
						}

						element.style.scrollMarginTop = "5rem";
						element.scrollIntoView(true);
					}
				}
			}
		} catch (error) {
			console.log(error.message);
		}
	}

	// Utility to wait for a condition to be true
	function waitFor(condition, timeout = 30000) {
		return new Promise((resolve, reject) => {
			const startTime = Date.now();

			function checkCondition() {
				const result = condition();
				if (result) {
					resolve(result);
				} else {
					if (Date.now() - startTime >= timeout) {
						reject(new Error("Timeout exceeded while waiting for condition."));
					} else {
						setTimeout(checkCondition, 1000);
					}
				}
			}

			checkCondition();
		});
	}
})();