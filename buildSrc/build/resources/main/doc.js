window.addEventListener('DOMContentLoaded', function() {
  console.log("dom loaded")
  processRows()
  setExampleToggleListeners()
})

function processRows(root, visible, depth = 1) {
  var selector = root ? root.dataset.node : "_root"
  var rows = document.querySelectorAll(`[data-node-parent="${selector}"]`)
  if(rows.length > 0) {
    if(root) {
      createExpandLink(root.children[0])
    }
    rows.forEach( row => {
      if(root) setVisibility(row, visible)
      if(row.children.length > 0) {
        row.children[0].style.paddingLeft = (depth * 18) + "px"
      }
      processRows(row, visible, depth + 1)
    })
  }
}

function createExpandLink(td) {
  var el = document.createElement("a")
  el.addEventListener("click", expandRows)
  el.href = "#"
  el.innerHTML = td.innerHTML
  td.innerHTML = ""
  td.appendChild(el)
}


function setExampleToggleListeners() {
  var elements = document.querySelectorAll([".toggablePre"])
  elements.forEach( el => {
    el.addEventListener("click", togglePreVisibility);
  })
}

function togglePreVisibility(event) {
  event.preventDefault()
  var preNode = this.parentNode.querySelector("pre")
  setVisibility(preNode, isElementInvisible(preNode))
}

function expandRows(event) {
  event.preventDefault()
  toggleVisibility(this.parentNode.parentNode)
}

function isElementInvisible(el) {
  return el.style.display == "none"
}

function toggleVisibility(r, visibility) {
  var rows = document.querySelectorAll(`[data-node-parent="${r.dataset.node}"]`)
  rows.forEach (row => {
    shouldBeVisible = visibility != undefined ? visibility : isElementInvisible(row)
    setVisibility(row, shouldBeVisible)
    if(!shouldBeVisible) {
      toggleVisibility(row, false)
    }
  })
}

function setVisibility(element, isVisible) {
    element.style.display = isVisible ? (element.tagName.toLowerCase() == "pre" ? "block" : "table-row") : "none"
}
