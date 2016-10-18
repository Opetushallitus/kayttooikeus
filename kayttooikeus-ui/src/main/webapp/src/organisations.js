import Bacon from 'baconjs'
import R from 'ramda'

import Dispatcher from './dispatcher'

const dispatcher = new Dispatcher()

const organisations = {
  toProperty: function(initialOrgs) {
    const orgsP = Bacon.update(initialOrgs,
      [dispatcher.stream('add')], addOrg,
      [dispatcher.stream('remove')], removeOrg,
    )
    return orgsP;

    function addOrg(orgs, newOrg) {
      return R.find(R.propEq('id', newOrg.id))(orgs) ? 
        orgs : orgs.concat([newOrg])
    }

    function removeOrg(orgs, orgIdToRemove) {
      return R.reject(org => org.id === orgIdToRemove, orgs)
    }
  },
  add: function(org) {
    dispatcher.push('add', org)
  },
  removeById: function(orgId) {
    dispatcher.push('remove', orgId)
  }
}

export default organisations