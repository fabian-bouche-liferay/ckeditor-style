package com.liferay.samples.fbo.ckeditor.styles;

import com.liferay.client.extension.constants.ClientExtensionEntryConstants;
import com.liferay.client.extension.model.ClientExtensionEntryRel;
import com.liferay.client.extension.service.ClientExtensionEntryRelLocalService;
import com.liferay.client.extension.type.GlobalCSSCET;
import com.liferay.client.extension.type.manager.CETManager;
import com.liferay.journal.constants.JournalPortletKeys;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.editor.configuration.BaseEditorConfigContributor;
import com.liferay.portal.kernel.editor.configuration.EditorConfigContributor;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.portlet.RequestBackedPortletURLFactory;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(
		property = {
				"editor.config.key=rich_text",
				"javax.portlet.name=" + JournalPortletKeys.JOURNAL
			},
		service = EditorConfigContributor.class
	)
public class StyleCKEditorConfigContributor extends BaseEditorConfigContributor {

	@Override
	public void populateConfigJSONObject(JSONObject jsonObject, Map<String, Object> inputEditorTaglibAttributes,
			ThemeDisplay themeDisplay, RequestBackedPortletURLFactory requestBackedPortletURLFactory) {

		JSONArray contentsCSSJSONArray = jsonObject.getJSONArray("contentsCss");
		
		contentsCSSJSONArray.put(themeDisplay.getMainCSSURL());

		contentsCSSJSONArray.put(HtmlUtil.escape(
				_portal.getStaticResourceURL(
						themeDisplay.getRequest(),
						_portal.getPathContext() + "/o/stylebook-variables?groupId="
						+ themeDisplay.getScopeGroupId())));

		for (ClientExtensionEntryRel clientExtensionEntryRel :
			_getClientExtensionEntryRels(themeDisplay.getScopeGroupId())) {

			GlobalCSSCET globalCSSCET = (GlobalCSSCET)_cetManager.getCET(
					clientExtensionEntryRel.getCompanyId(),
					clientExtensionEntryRel.getCETExternalReferenceCode());

			contentsCSSJSONArray.put(
					HtmlUtil.escape(
						_portal.getStaticResourceURL(
							themeDisplay.getRequest(),
							_portal.getPathContext() +
							globalCSSCET.getURL())));



		}

	}
	
	private List<ClientExtensionEntryRel> _getClientExtensionEntryRels(
		long groupId) {

		List<ClientExtensionEntryRel> clientExtensionEntryRels =
				new ArrayList<>();

		try {
			clientExtensionEntryRels.addAll(getClientExtensionEntryRels(_layoutSetLocalService.getLayoutSet(groupId, false)));
		} catch (PortalException e) {
			if(_log.isDebugEnabled()) {
				_log.debug("No public layoutset for groupId " + groupId);
			}
		}

		try {
			clientExtensionEntryRels.addAll(getClientExtensionEntryRels(_layoutSetLocalService.getLayoutSet(groupId, true)));
		} catch (PortalException e) {
			if(_log.isDebugEnabled()) {
				_log.debug("No private layoutset for groupId " + groupId);
			}
		}

		return clientExtensionEntryRels;
	}
	
	private List<ClientExtensionEntryRel> getClientExtensionEntryRels(LayoutSet layoutSet) {
		return _clientExtensionEntryRelLocalService.
					getClientExtensionEntryRels(
						_portal.getClassNameId(LayoutSet.class),
						layoutSet.getLayoutSetId(),
						ClientExtensionEntryConstants.TYPE_GLOBAL_CSS,
						QueryUtil.ALL_POS, QueryUtil.ALL_POS);
	}

	@Reference
	private CETManager _cetManager;
		
	@Reference
	private Portal _portal;
	
	@Reference
	private LayoutSetLocalService _layoutSetLocalService;
	
	@Reference
	private ClientExtensionEntryRelLocalService
		_clientExtensionEntryRelLocalService;
	
	private static final Log _log = LogFactoryUtil.getLog(StyleCKEditorConfigContributor.class);

}
