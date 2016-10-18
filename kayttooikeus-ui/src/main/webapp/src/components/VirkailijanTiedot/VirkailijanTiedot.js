import React from 'react'

class VirkailijanTiedot extends React.Component {
  constructor() {
    super()
    this.renderKieli = this.renderKieli.bind(this)
  }

  render() {
    const L = this.props.l10n 
    return (
      <fieldset>
        <h2>Virkailijan tiedot</h2>
        <div>
          <label htmlFor="email">{L['VIRKAILIJAN_TIEDOT_SPOSTI']}</label>
          <input type="text" id="email"/>
        </div>
        <div>
          <label htmlFor="lang">{L['VIRKAILIJAN_TIEDOT_KIELI']}</label>
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
