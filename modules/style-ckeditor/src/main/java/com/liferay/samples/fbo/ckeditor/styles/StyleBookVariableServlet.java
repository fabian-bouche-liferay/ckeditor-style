package com.liferay.samples.fbo.ckeditor.styles;

import com.liferay.frontend.css.variables.ScopedCSSVariables;
import com.liferay.frontend.css.variables.ScopedCSSVariablesProvider;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerListFactory;
import com.liferay.osgi.service.tracker.collections.map.PropertyServiceReferenceComparator;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(
		immediate = true,
		property = {
			"osgi.http.whiteboard.context.path=/",
			"osgi.http.whiteboard.servlet.pattern=/stylebook-variables"
		},
		service = Servlet.class
	)
public class StyleBookVariableServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws ServletException, IOException {

		httpServletResponse.setContentType("text/css");
		
		long groupId = ParamUtil.get(httpServletRequest, "groupId", -1);
		if(groupId != -1) {
		
			Group group;
			try {
				group = _groupLocalService.getGroup(groupId);
				long plid = group.getDefaultPublicPlid();
				if(plid == -1) {
					plid = group.getDefaultPrivatePlid();
				}
				
				if(plid != -1) {
					ThemeDisplay themeDisplay = new ThemeDisplay();
					themeDisplay.setScopeGroupId(groupId);
					themeDisplay.setLayout(_layoutLocalService.getLayout(plid));
					themeDisplay.setSiteGroupId(groupId);

					PrintWriter printWriter = httpServletResponse.getWriter();

					for (ScopedCSSVariablesProvider scopedCSSVariablesProvider :
							_scopedCSSVariablesProviders) {

						httpServletRequest.setAttribute(WebKeys.THEME_DISPLAY, themeDisplay);
						
						_writeCSSVariables(
							printWriter,
							scopedCSSVariablesProvider.getScopedCSSVariablesCollection(
								httpServletRequest));
					}			
				}
				
			} catch (PortalException e) {
				if(_log.isWarnEnabled()) {
					_log.warn("No group found with groupId " + groupId);
				}
			}
			
		}
		
	}
	
	@Activate
	protected void activate(BundleContext bundleContext) {
		_scopedCssVariablesProviderServiceTrackerList =
			ServiceTrackerListFactory.open(
				bundleContext, ScopedCSSVariablesProvider.class,
				new PropertyServiceReferenceComparator<>("service.ranking"));

		setScopedCSSVariablesProviders(
			_scopedCssVariablesProviderServiceTrackerList);
	}

	@Deactivate
	protected void deactivate() {
		setScopedCSSVariablesProviders(null);

		_scopedCssVariablesProviderServiceTrackerList.close();

		_scopedCssVariablesProviderServiceTrackerList = null;
	}
	
	protected void setScopedCSSVariablesProviders(
		Iterable<ScopedCSSVariablesProvider> scopedCSSVariablesProviders) {

		_scopedCSSVariablesProviders = scopedCSSVariablesProviders;
	}
	
	private void _writeCSSVariables(
		PrintWriter printWriter,
		Collection<ScopedCSSVariables> scopedCSSVariablesCollection) {

		for (ScopedCSSVariables scopedCSSVariables :
				scopedCSSVariablesCollection) {

			printWriter.print(StringPool.TAB);
			printWriter.print(scopedCSSVariables.getScope());
			printWriter.print(" {\n");

			Map<String, String> cssVariables =
				scopedCSSVariables.getCSSVariables();

			for (Map.Entry<String, String> entry : cssVariables.entrySet()) {
				printWriter.print("\t\t--");
				printWriter.print(entry.getKey());
				printWriter.print(": ");
				printWriter.print(entry.getValue());
				printWriter.print(";\n");
			}

			printWriter.print("\t}\n");
		}
	}
	
	private Iterable<ScopedCSSVariablesProvider> _scopedCSSVariablesProviders;
	private ServiceTrackerList<ScopedCSSVariablesProvider>
		_scopedCssVariablesProviderServiceTrackerList;

	@Reference
	private LayoutLocalService _layoutLocalService;

	@Reference
	private GroupLocalService _groupLocalService;
	
	private static final Log _log = LogFactoryUtil.getLog(StyleBookVariableServlet.class);

}
