---
hide:
  - toc
---

# Serialization

<style>
.md-typeset h3 {
    background: var(--md-accent-fg-color--light);
    padding: 10px;
}

.md-typeset .big-table3 {
    /* Tables with lots of content in 3 columns */
    font-size: medium;
    word-break: normal;
    display: grid;
    grid-template-columns: max-content max-content auto;
    margin-top: 20px;
}

.md-typeset .big-table3 p,
.md-typeset .big-table6 p {
    margin-top: 0;
}

.md-typeset .big-table6 {
    /* Tables with lots of content in 6 columns*/
    font-size: medium;
    word-break: normal;
    display: grid;
    grid-template-columns: 10px 10px 10px minmax(min-content, 25%) minmax(min-content,25%) auto;
    margin-top: 20px;
}

/* divs inside big-table are actually cells */
.md-typeset .big-table3 div,
.md-typeset .big-table6 div {
    padding-left: 10px;
    vertical-align: top;
    border-top: 1px solid var(--md-accent-fg-color--light);
}

.md-typeset__table,
.md-typeset__table tbody {
    display: table;
    width: 100%;
    margin: 0;
}

.side-info {
    float: right;
}

.side-info td {
    background-color: var(--md-accent-fg-color--light);
}

.md-typeset table:not([class]) {
    border: none;
}
.md-typeset table:not([class]) td {
    border: none;
    padding-top: 0.25rem;
    padding-bottom: 0.25rem;
}
.md-typeset table:not([class]) td:has(dl) {
    padding-left: 0;
}
</style>
