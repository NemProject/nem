const javadocOddRowColor = "odd-row-color";
const javadocEvenRowColor = "even-row-color";
const javadocTableTab = "table-tab";
const javadocActiveTableTab = "active-table-tab";

function toggleJavadocStyle(classList, condition, trueStyle, falseStyle) {
	if (condition) {
		classList.remove(falseStyle);
		classList.add(trueStyle);
	} else {
		classList.remove(trueStyle);
		classList.add(falseStyle);
	}
}

function updateTabs(tableId, selected) {
	document.getElementById(`${tableId}.tabpanel`)
		.setAttribute("aria-labelledby", selected);

	document.querySelectorAll(`button[id^="${tableId}"]`)
		.forEach((tab, index) => {
			if (selected === tab.id || (tableId === selected && 0 === index)) {
				tab.className = javadocActiveTableTab;
				tab.setAttribute("aria-selected", true);
				tab.setAttribute("tabindex", 0);
			} else {
				tab.className = javadocTableTab;
				tab.setAttribute("aria-selected", false);
				tab.setAttribute("tabindex", -1);
			}
		});
}

function show(tableId, selected, columns) {
	if (tableId !== selected) {
		document.querySelectorAll(`div.${tableId}:not(.${selected})`)
			.forEach((element) => {
				element.style.display = "none";
			});
	}

	document.querySelectorAll(`div.${selected}`)
		.forEach((element, index) => {
			element.style.display = "";
			const isEvenRow = index % (columns * 2) < columns;
			toggleJavadocStyle(
				element.classList,
				isEvenRow,
				javadocEvenRowColor,
				javadocOddRowColor
			);
		});

	updateTabs(tableId, selected);
}

function switchTab(event) {
	const tabs = event.target.closest(".table-tabs");
	if (!tabs)
		return;

	const selected = tabs.querySelector("[aria-selected=true]");
	if (!selected)
		return;

	if ((37 === event.keyCode || 38 === event.keyCode) && selected.previousElementSibling) {
		selected.previousElementSibling.click();
		selected.previousElementSibling.focus();
		event.preventDefault();
	} else if ((39 === event.keyCode || 40 === event.keyCode) && selected.nextElementSibling) {
		selected.nextElementSibling.click();
		selected.nextElementSibling.focus();
		event.preventDefault();
	}
}
