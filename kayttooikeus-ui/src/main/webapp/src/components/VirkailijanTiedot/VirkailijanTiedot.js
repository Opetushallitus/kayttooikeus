import React from 'react'

class VirkailijanTiedot extends React.Component {
  constructor() {
    super()
    this.renderKieli = this.renderKieli.bind(this)
  }

  render() {
    return (
      <fieldset>
        <h2>Virkailijan tiedot</h2>
        <div>
          <label htmlFor="email">Sähköposti</label>
          <input type="text" id="email"/>
        </div>
        <div>
          <label htmlFor="lang">Kieli</label>
          <select id="lang">
            {this.props.kielet.map(this.renderKieli)}
          </select>
        </div>
      </fieldset>
    );
  }

  renderKieli(kieli) {
    return (
      <option key={kieli.code} value={kieli.code}>{kieli['name-' + this.props.uiLang]}</option>  
    )
  }
}

export default VirkailijanTiedot;
