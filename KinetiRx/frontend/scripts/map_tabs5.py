import sys; sys.path.insert(0,'.')
from i18n_tool import run, text, attr, lit, raw
SRC='../src/components/tabs/'
def add_import(path, line):
    s=open(path,encoding='utf-8').read()
    if line not in s:
        import re
        m=list(re.finditer(r'^import [^;]*;\n', s, re.M|re.S))
        pos=m[-1].end()
        s=s[:pos]+line+'\n'+s[pos:]
        open(path,'w',encoding='utf-8').write(s)

# ---------------- PatientsTab
run(SRC+'PatientsTab.tsx','patients',[
 text('Patient Profile Master Database','قاعدة بيانات ملفات المرضى','title'),
 text('Create and maintain master patient profiles with doctor assignments, address, mobile numbers, and comprehensive medical visit history.','إنشاء وإدارة ملفات المرضى مع الأطباء المعالجين والعنوان وأرقام الجوال وسجل الزيارات الطبية الكامل.','subtitle'),
 text('Export Excel','تصدير Excel','exportExcel'),
 text('Scan Patient ID','مسح رقم المريض','scanId'),
 text('+ Register Patient Profile','+ تسجيل ملف مريض','register'),
 text('Total Registered Profiles','إجمالي الملفات المسجلة','totalProfiles'),
 text('Active medical profiles in pharmacy','ملفات طبية نشطة في الصيدلية','totalProfilesHint'),
 text('Total Outstanding Dues','إجمالي المستحقات المتبقية','totalDues'),
 text('Auto-Linked to Special Orders','مرتبطة تلقائياً بالطلبات الخاصة','autoLinked'),
 text('Instant Fetch','استرجاع فوري','instantFetch'),
 text('Type Patient ID to auto-populate orders & POS','اكتب رقم المريض لتعبئة الطلبات ونقطة البيع تلقائياً','instantFetchHint'),
 text('Patient ID','رقم المريض','colId'),
 text('Full Name','الاسم الكامل','colName'),
 text('Mobile Number','رقم الجوال','colMobile'),
 text('Age / Gender','العمر / الجنس','colAgeGender'),
 text('Address','العنوان','colAddress'),
 text('Assigned Doctor','الطبيب المعالج','colDoctor'),
 text('Profile & WhatsApp','الملف والواتساب','colActions'),
 text('No matching patient profiles found. Click "+ Register Patient Profile" to add one.','لا توجد ملفات مرضى مطابقة. اضغط "+ تسجيل ملف مريض" لإضافة ملف.','empty'),
 text('WhatsApp','واتساب','whatsapp'),
 text('Profile CV','السيرة الطبية','profileCv'),
 attr('placeholder','Search by Patient ID (e.g. P-101 or 101), Name, Mobile Number, Address, Doctor...','ابحث برقم المريض (مثال: P-101 أو 101) أو الاسم أو الجوال أو العنوان أو الطبيب...','searchPlaceholder'),
 attr('title','Send WhatsApp Community Invite','إرسال دعوة الانضمام للمجتمع عبر واتساب','sendInvite'),
 attr('title','View Complete Patient CV & History','عرض السيرة الطبية والسجل الكامل للمريض','viewCv'),
 attr('title','Edit Patient Profile','تعديل ملف المريض','editProfile'),
 attr('title','Delete Patient Record','حذف سجل المريض','deleteProfile'),
 lit('Invalid phone number for WhatsApp invite.','رقم الهاتف غير صحيح لإرسال دعوة واتساب.','invalidInvitePhone'),
 lit('Showing Due Only','عرض المستحقات فقط','showingDues'),
 lit('Filter Dues Only','تصفية المستحقات فقط','filterDues'),
],hook_after="  const currencySymbol = getCurrencySymbol(invoiceConfig?.currency);")

# ---------------- MedicineOrdersTab (status labels for display only)
run(SRC+'MedicineOrdersTab.tsx','orders',[
 text('Special Medicine Need Orders','طلبات الأدوية الخاصة','title'),
 text('Manage out-of-stock emergency patient prescriptions linked with Patient Profiles and assigned to distributors with promised delivery dates.','إدارة وصفات المرضى الطارئة للأدوية غير المتوفرة، مرتبطةً بملفات المرضى ومُسندةً إلى الموزعين مع مواعيد التسليم المتفق عليها.','subtitle'),
 text('Export Excel','تصدير Excel','exportExcel'),
 text('+ Note Special Medicine Order','+ تسجيل طلب دواء خاص','addOrder'),
 text('Generate Purchase Order (by Distributor)','إنشاء أمر شراء (حسب الموزع)','generatePo'),
 text('Print PO','طباعة أمر الشراء','printPo'),
 text('Download','تنزيل','download'),
 text('WhatsApp','واتساب','whatsapp'),
 text('Status:','الحالة:','statusLabel'),
 text('All Statuses','كل الحالات','allStatuses'),
 text('Distributor Ordered','طلب من الموزع','optDistributorOrdered'),
 text('Processing','قيد المعالجة','optProcessing'),
 text('Pending','معلّق','optPending'),
 text('Delivered','تم التسليم','optDelivered'),
 text('Cancelled','ملغى','optCancelled'),
 text('Medicine Required','الدواء المطلوب','colMedicine'),
 text('Patient Details','بيانات المريض','colPatient'),
 text('Distributor Assigned','الموزع المُسند','colDistributor'),
 text('Commitment Delivery Time','موعد التسليم المتفق عليه','colDelivery'),
 text('Quantity','الكمية','colQty'),
 text('Order Status','حالة الطلب','colStatus'),
 text('Edit','تعديل','edit'),
 text('No special need medicine orders found. Click "+ Note Special Medicine Order" to create one.','لا توجد طلبات أدوية خاصة. اضغط "+ تسجيل طلب دواء خاص" لإنشاء طلب.','empty'),
 text('Walk-in','زيارة مباشرة','walkIn'),
 attr('title','Print the Purchase Order and mark these items as Distributor Ordered','طباعة أمر الشراء وتعيين هذه الأصناف كطلب من الموزع','printPoHint'),
 attr('title','Share PO summary via WhatsApp and mark these items as Distributor Ordered','مشاركة ملخص أمر الشراء عبر واتساب وتعيين هذه الأصناف كطلب من الموزع','whatsappPoHint'),
 attr('placeholder','Search by Patient ID (e.g. P-101 or 101), Patient Name, Medicine, Phone, or Distributor...','ابحث برقم المريض (مثال: P-101 أو 101) أو الاسم أو الدواء أو الهاتف أو الموزع...','searchPlaceholder'),
 attr('title','Click to view Patient Profile CV','اضغط لعرض السيرة الطبية للمريض','viewCv'),
 attr('title','Edit Order','تعديل الطلب','editOrder'),
])

# ---------------- OPDTab
run(SRC+'OPDTab.tsx','opd',[
 text('OPD & Lab Clinical Re-visit Register','سجل العيادة الخارجية والمتابعة المخبرية','title'),
 text('Doctor consultation logs and automatic 3-4 days advance follow-up revisit reminders with direct SMS & Call triggers.','سجل استشارات الأطباء وتذكيرات المتابعة التلقائية قبل 3 إلى 4 أيام، مع إجراءات مباشرة بالرسائل والاتصال.','subtitle'),
 text('Export Excel','تصدير Excel','exportExcel'),
 text('+ Add OPD Patient','+ إضافة مريض عيادة خارجية','addVisit'),
 text('Patient Details','بيانات المريض','colPatient'),
 text('Doctor Assigned','الطبيب المعالج','colDoctor'),
 text('Visit Date','تاريخ الزيارة','colVisitDate'),
 text('Next Re-Visit Date','تاريخ المتابعة القادمة','colRevisit'),
 text('Tests Advised','التحاليل الموصى بها','colTests'),
 text('Alert Status','حالة التنبيه','colAlert'),
 text('SMS & Phone Call','رسالة نصية واتصال','colActions'),
 text('No OPD consultation visits recorded.','لا توجد زيارات استشارية مسجلة في العيادة الخارجية.','empty'),
 text('3-4 Days Advance','قبل 3 إلى 4 أيام','alertAdvance'),
 text('Auto SMS','رسالة تلقائية','autoSms'),
 text('Call','اتصال','call'),
],hook_after=None,hook=False)

# ---------------- BusinessDevTab
run(SRC+'BusinessDevTab.tsx','bizdev',[
 text('Doctor Campaign & Marketing','حملات الأطباء والتسويق','title'),
 text('Publicity, miking and leafleting schedule (7 Days Advance Alert)','جدول الدعاية والإعلان بالمكبرات والمنشورات (تنبيه قبل 7 أيام)','subtitle'),
 text('+ Add Campaign','+ إضافة حملة','addCampaign'),
 text('No active marketing campaigns.','لا توجد حملات تسويقية نشطة.','emptyCampaigns'),
 text('Edit','تعديل','edit'),
 text('Monthly Tasks & Maintenance','المهام الشهرية والصيانة','tasksTitle'),
 text('Hardware repairs, PC service, staff meetings, equipment checks','إصلاح الأجهزة وصيانة الحواسيب واجتماعات الموظفين وفحص المعدات','tasksSubtitle'),
 text('+ Add Task','+ إضافة مهمة','addTask'),
 text('No maintenance tasks logged.','لا توجد مهام صيانة مسجلة.','emptyTasks'),
],hook_after=None,hook=False)

# ---------------- EmployeeMgmtTab
run(SRC+'EmployeeMgmtTab.tsx','employees',[
 text('Employee & Admin Access Control','التحكم في الموظفين وصلاحيات المدير','title'),
 text('Configure employee PIN passwords, designate responsibilities, and manage master administrative credentials.','ضبط الرموز السرية للموظفين وتحديد المسؤوليات وإدارة بيانات دخول المدير.','subtitle'),
 text('Change Master Admin Password','تغيير كلمة مرور المدير','changeAdminPassword'),
 text('+ Add New Employee','+ إضافة موظف جديد','addEmployee'),
 text('Administrator Security Master Key','المفتاح الأمني للمدير','adminKey'),
 text('Auto-Saved & Active','محفوظ تلقائياً ومفعّل','autoSaved'),
 text('This master password protects system reset, offline backups, sensitive overrides, and terminal authentication.','تحمي كلمة المرور الرئيسية إعادة ضبط النظام والنسخ الاحتياطية والتعديلات الحساسة والتحقق من الأجهزة.','adminKeyHint'),
 text('Update Password','تحديث كلمة المرور','updatePassword'),
 text('Employee Name','اسم الموظف','colName'),
 text('Designation / Role','المسمى / الدور','colRole'),
 text('Login Password / PIN','كلمة المرور / الرمز السري','colPassword'),
 text('Permitted Modules','الوحدات المسموحة','colModules'),
 text('Actions','إجراءات','colActions'),
 text('Edit Permissions','تعديل الصلاحيات','editPermissions'),
 text('Remove','حذف','remove'),
],hook_after=None,hook=False)
print("done")
