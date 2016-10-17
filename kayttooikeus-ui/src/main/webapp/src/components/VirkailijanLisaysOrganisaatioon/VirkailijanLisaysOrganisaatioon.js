import React from 'react'

class VirjailijanLisaysOrganisaatioon extends React.Component {
  constructor() {
    super()
    this.renderOrganisaatio = this.renderOrganisaatio.bind(this)
    this.renderKayttooikeus = this.renderKayttooikeus.bind(this)
    this.selectOrganisaatio = this.selectOrganisaatio.bind(this)
    this.state = {
      selectedOrganisaatio: ''
    }
  }

  render() {
    const activeOrg = this.props.organisaatiot
      .filter(org => org.id === this.state.selectedOrganisaatio)
    const kayttooikeudet = activeOrg.length === 1 ? 
      activeOrg[0].permissions : this.props.organisaatiot[0].permissions

    return (
      <fieldset>
        <h2>{this.props.l10n['VIRKAILIJAN_LISAYS_ORGANISAATIOON_OTSIKKO']}</h2>
        <div>
          <label htmlFor="org">{this.props.l10n['VIRKAILIJAN_LISAYS_ORGANISAATIOON_ORGANISAATIO']}</label>
          <select id="org" onChange={this.selectOrganisaatio}>
            {this.props.organisaatiot.map(this.renderOrganisaatio)}
          </select>
        </div>
        <div>
          <label htmlFor="kayttooikeudet">{this.props.l10n['VIRKAILIJAN_LISAYS_ORGANISAATIOON_MYONNA_KAYTTOOIKEUKSIA']}</label>
          <select id="kayttooikeudet">
            {kayttooikeudet.map(this.renderKayttooikeus)}
          </select>
        </div>
      </fieldset>
    );
  }

  selectOrganisaatio(e) {
    this.setState({
      selectedOrganisaatio: e.target.value
    }) 
  }

  renderOrganisaatio(org) {
    return (
       <option key={org.id} value={org.id}>{org['name-' + this.props.uiLang]}</option>  
    )
  }

  renderKayttooikeus(perm) {
    return (
      <option key={perm.id} value={perm.id}>{perm['name-' + this.props.uiLang]}</option>  
    )
  }

}

export default VirjailijanLisaysOrganisaatioon;
