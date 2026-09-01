import DefaultTheme from 'vitepress/theme'
import Mermaid from './MermaidDiagram.vue'
import './custom.css'

export default {
  extends: DefaultTheme,

  enhanceApp({ app }) {
    app.component('Mermaid', Mermaid)
  }
}