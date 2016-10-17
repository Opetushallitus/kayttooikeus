import React from 'react'
import './VirkailijanLisaysLomake.css'

import VirkailijanTiedot from '../VirkailijanTiedot/VirkailijanTiedot'
import VirkailijanLisaysOrganisaatioon from '../VirkailijanLisaysOrganisaatioon/VirkailijanLisaysOrganisaatioon'

class VirkailijanLisaysLomake extends React.Component {
  render() {
    const uiLang = 'fi'
    const l10n = this.props.l10n[uiLang]
    return (
      <form>
        <h1>{l10n['VIRKAILIJAN_LISAYS_OTSIKKO']}</h1>
        <VirkailijanTiedot 
          kielet={this.props.languages}
          l10n={l10n}
          uiLang={uiLang} />
        <VirkailijanLisaysOrganisaatioon
          organisaatiot={this.props.organisations}
          l10n={l10n}
          uiLang={uiLang} />
        <button onClick={this.handleSubmit}>{l10n['VIRKAILIJAN_LISAYS_TALLENNA']}</button>
      </form>
    );
  }

  handleSubmit(e) {
    e.preventDefault()
  }
}

export default VirkailijanLisaysLomake;