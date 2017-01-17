import React from 'react'

import organisations from '../../organisations'

import LisatytOrganisaatiot from '../LisatytOrganisaatiot/LisatytOrganisaatiot'

class VirjailijanLisaysOrganisaatioon extends React.Component {
  constructor() {
    super()
    this.state = {
      selectedOrganisaatio: '',
      selectedKayttooikeuses: [],
    }
  }

  componentWillMount() {
    this.setState({
      selectedOrganisaatio: this.props.organisaatiot[0].id
    })
  }

  render() {
    const L = this.props.l10n 

    const activeOrg = this.props.organisaatiot
      .filter(org => org.id === this.state.selectedOrganisaatio)
    const kayttooikeudet = activeOrg.length === 1 ? 
      activeOrg[0].permissions : this.props.organisaatiot[0].permissions

    return (
      <fieldset>
        <h2>{L['VIRKAILIJAN_LISAYS_ORGANISAATIOON_OTSIKKO']}</h2>
        <div>
          <label htmlFor="org">
            {L['VIRKAILIJAN_LISAYS_ORGANISAATIOON_ORGANISAATIO']}
          </label>
          <select id="org" onChange={this.selectOrganisaatio.bind(this)}>
            {this.props.organisaatiot.map(this.renderOrganisaatio.bind(this))}
          </select>
        </div>
        <div>
          <label htmlFor="kayttooikeudet">
            {L['VIRKAILIJAN_LISAYS_ORGANISAATIOON_MYONNA_KAYTTOOIKEUKSIA']}
          </label>
          <select onChange={this.selectKayttooikeudet.bind(this)} multiple id="kayttooikeudet">
            {kayttooikeudet.map(this.renderKayttooikeus.bind(this))}
          </select>
        </div>
        <a href="" onClick={this.addOrganisaatio.bind(this)}>
          {L['VIRKAILIJAN_LISAYS_ORGANISAATIOON_LISAA']}
        </a>
        <LisatytOrganisaatiot addedOrgs={this.props.addedOrgs} />
      </fieldset>
    )
  }

  addOrganisaatio(e) {
    e.preventDefault()
    organisations.add({
      id: this.state.selectedOrganisaatio,
      permissions: this.state.selectedKayttooikeuses
    })
  }

  selectOrganisaatio(e) {
    this.setState({
      selectedOrganisaatio: e.target.value,
      selectedKayttooikeuses: []
    }) 
  }

  selectKayttooikeudet(e) {
    this.setState({
      selectedKayttooikeuses: Array.apply(null, e.target.options)
        .filter(option => option.selected)
        .map(option => option.value)
    })
  }

  renderOrganisaatio(org) {
    return (
      <option key={org.id} value={org.id}>
        {org['name-' + this.props.uiLang]}
      </option>  
    )
  }

  renderKayttooikeus(kayttooikeus) {
    return (
      <option key={kayttooikeus.id} value={kayttooikeus.id}>
        {kayttooikeus['name-' + this.props.uiLang]}
      </option>  
    )
  }

}

export default VirjailijanLisaysOrganisaatioon
