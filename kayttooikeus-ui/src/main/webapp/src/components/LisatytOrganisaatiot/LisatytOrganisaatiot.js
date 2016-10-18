import React from 'react'

import './LisatytOrganisaatiot.css'

import organisations from '../../organisations'

class LisatytOrganisaatiot extends React.Component {
  render() {
    return (
      <div>
        {this.props.addedOrgs.map(this.renderAddedOrganisaatio.bind(this))}
      </div>
    )
  }

  renderAddedOrganisaatio(addedOrg) {
    return (
      <div className="added-org" key={addedOrg.id}>
        <h3>{addedOrg.id} 
          <a href="" onClick={this.removeAddedOrg.bind(this, addedOrg.id)}>X</a>
        </h3>
        <ul> 
          {addedOrg.permissions.map(permission => {
            return <li key={permission}>{permission}</li>
          })}
        </ul>
      </div>
    )
  }

  removeAddedOrg(id, e) {
    e.preventDefault()
    organisations.removeById(id)
  }

}

export default LisatytOrganisaatiot
