import React from 'react'
import PureRenderMixin from 'react-addons-pure-render-mixin';
import R from 'ramda'
import $ from 'jquery'

import {addEmptyOrganization, changeOrganization} from '../../logic/organisations'
import AddedOrganisations from './AddedOrganisations'

export const repositionSelectionArrows = () => {
  $('fieldset.add-to-organisation .select2-selection__arrow').each((i, e) => {
    let container = $(e).parent().parent().parent();
    if (container.width() !== 250) {
      $(e).css({
        'right': 18
      })
    }
  });
}

const AddToOrganisation = React.createClass({
  mixins: [PureRenderMixin],
  
  render: function() {
    const L = this.props.l10n;
    return (
      <fieldset className="add-to-organisation">
        <h2>{L['VIRKAILIJAN_LISAYS_ORGANISAATIOON_OTSIKKO']}</h2>

        <AddedOrganisations changeOrganization={oldId => e => this.changeOrganization(oldId, e)} orgs={this.props.orgs}
            addedOrgs={this.props.addedOrgs} l10n={this.props.l10n} uiLang={this.props.uiLang} />
        <div className="row">
          <a href="#" onClick={this.addEmptyOrganization}>{L['VIRKAILIJAN_KUTSU_LISAA_ORGANISAATIO_LINKKI']}</a>
        </div>
      </fieldset>
    )
  },

  addEmptyOrganization: function(e) {
    e.preventDefault();
    addEmptyOrganization();
    setTimeout(() => repositionSelectionArrows(), 2000);
  },

  changeOrganization: function(oldOid, e) {
    const selectedOrganization = R.find(R.pathEq(['oid'], e.target.value))(this.props.orgs);
    if (selectedOrganization) {
      changeOrganization(oldOid, selectedOrganization, this.props.omaOid);

      // TODO: try to listen to when the input box size actually changes instead of having a hardcoded timeout
      setTimeout(() => {
        let parent = $(`option[value="${selectedOrganization.oid}"]`).parent().parent();
        let removeIcon = parent.find('.remove-icon.after');

        repositionSelectionArrows();

        // reposition remove icon
        removeIcon.css({
          'margin-left': 15,
        });
      }, 2000)
    }
  }
});

export default AddToOrganisation;
