---
hide:
  - toc
---

<div id="scalar-api-reference"></div>

<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@scalar/api-reference/dist/style.css" />
<script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
<script>
  const container = document.getElementById('scalar-api-reference');
  let scalarInstance = null;
  let isRendering = false;
  const observer = new MutationObserver(() => {
    if (isRendering) return;
    renderScalar();
  });
  function renderScalar() {
    isRendering = true;
    if (scalarInstance) {
      observer.disconnect();
      scalarInstance.destroy();
    }
    scalarInstance = Scalar.createApiReference(container, {
      url: new URL('../openapi3.yml', window.location.href).toString(),
      slug: 'openapi3',
      title: 'NEM REST API',
      theme: 'purple',
      showDeveloperTools: 'never',
      hideSearch: true,
      hideClientButton: true,
      agent: { disabled: true },
      mcp: { disabled: true },
      telemetry: false,
      generateTagSlug: (tag) => `${tag.name.replaceAll(' ', '_')}`,
      generateOperationSlug: (operation) => `${operation.operationId.replaceAll('/', '')}`,
      hideDarkModeToggle: true,
      forceDarkModeState: document.body.getAttribute('data-md-color-scheme') === 'slate' ? 'dark' : 'light',
      onLoaded: () => {
        isRendering = false;
        observer.observe(document.body, {
          attributes: true,
          attributeFilter: ['data-md-color-scheme']
        });
      }
    })
  };
  window.addEventListener('load', renderScalar);
</script>
<style>
#scalar-api-reference {
  /* Under the header */
  position: relative;
  z-index: 0;
}
#scalar-api-reference [id] {
  /* So it shows below the header */
  scroll-margin-top: 3rem;
}
.md-main__inner {
  /* Use all available width */
  max-width: none;
  margin: 0;
}
</style>
